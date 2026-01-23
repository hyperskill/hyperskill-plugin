package org.hyperskill.academy.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.coursecreator.AdditionalFilesUtils.collectAdditionalFiles
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.configuration.EduConfiguratorManager.findConfigurator
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.updateDescriptionTextAndFormat
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.invokeLater
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.configFileName
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.mapper
import org.hyperskill.academy.learning.yaml.YamlLoader.deserializeContent
import org.hyperskill.academy.learning.yaml.errorHandling.RemoteYamlLoadingException
import org.hyperskill.academy.learning.yaml.errorHandling.loadingError
import org.hyperskill.academy.learning.yaml.format.getRemoteChangeApplierForItem
import org.hyperskill.academy.learning.yaml.migrate.ADDITIONAL_FILES_COLLECTOR_MAPPER_KEY
import org.hyperskill.academy.learning.yaml.migrate.YAML_VERSION_MAPPER_KEY
import org.hyperskill.academy.learning.yaml.migrate.YamlMigrator
import org.jetbrains.annotations.NonNls

object YamlDeepLoader {
  private val HYPERSKILL_PROJECT_REGEX = "$HYPERSKILL_PROJECTS_URL/(\\d+)/.*".toRegex()
  private val LOG = Logger.getInstance(YamlDeepLoader::class.java)

  @Throws(RemoteYamlLoadingException::class)
  fun loadCourse(project: Project): Course? {
    val projectDir = project.courseDir

    @NonNls
    val errorMessageToLog = "Course yaml config cannot be null"
    val courseConfig = projectDir.findChild(YamlConfigSettings.COURSE_CONFIG) ?: error(errorMessageToLog)

    // the initial mapper has no idea whether the course is in the CC or in the Student mode
    val initialMapper = YamlMapper.basicMapper()
    initialMapper.setupForMigration(project)
    val deserializedItem = deserializeItemProcessingErrors(courseConfig, project, mapper = initialMapper)
    if (deserializedItem == null) {
      LOG.warn("Failed to deserialize course from ${courseConfig.path}: deserializeItemProcessingErrors returned null")
      return null
    }
    val deserializedCourse = deserializedItem as? Course
    if (deserializedCourse == null) {
      LOG.warn("Failed to cast deserialized item to Course from ${courseConfig.path}: actual type is ${deserializedItem::class.simpleName}")
      return null
    }

    val migrator = YamlMigrator(initialMapper)
    val needMigration = migrator.needMigration()

    // Check if course YAML version is newer than supported and show notification
    val courseYamlVersion = initialMapper.getEduValue(YAML_VERSION_MAPPER_KEY)
    if (courseYamlVersion != null && courseYamlVersion > YamlMapper.CURRENT_YAML_VERSION) {
      showYamlVersionCompatibilityNotification(project, courseYamlVersion, YamlMapper.CURRENT_YAML_VERSION)
    }

    // this mapper already respects course mode, it will be used to deserialize all other course items
    val mapper = mapper()
    mapper.setupForMigration(project)
    mapper.setEduValue(YAML_VERSION_MAPPER_KEY, initialMapper.getEduValue(YAML_VERSION_MAPPER_KEY))

    deserializedCourse.items = deserializedCourse.deserializeContent(project, deserializedCourse.items, mapper)
    deserializedCourse.items.forEach { deserializedItem ->
      when (deserializedItem) {
        is Section -> {
          // set parent to correctly obtain dirs in deserializeContent method
          deserializedItem.parent = deserializedCourse
          deserializedItem.items = deserializedItem.deserializeContent(project, deserializedItem.items, mapper)
          deserializedItem.lessons.forEach {
            it.parent = deserializedItem
            it.items = it.deserializeContent(project, it.taskList, mapper)
          }
        }

        is Lesson -> {
          // set parent to correctly obtain dirs in deserializeContent method
          deserializedItem.parent = deserializedCourse
          deserializedItem.items = deserializedItem.deserializeContent(project, deserializedItem.taskList, mapper)
          addNonEditableFilesToCourse(deserializedItem, deserializedCourse, project)
          deserializedItem.removeNonExistingTaskFiles(project)
        }
      }
    }

    if (needMigration) {
      project.invokeLater {
        // After migration, we save all YAMLs back to disk.
        // In theory, org.hyperskill.academy.learning.yaml.YamlLoader.loadItem() could be fired before the migrated YAMLs are saved,
        // and that could lead to incorrectly read YAML.
        // One of the dangerous places: the FileEditorManagerListener calls loadItem() to refresh editor notifications for
        // YAML files, and this happens right after the project is loaded.
        YamlFormatSynchronizer.saveAll(project)
      }
    }

    // we initialize course before setting description and remote info, as we have to set parent item
    // to obtain description/remote config file to set info from
    deserializedCourse.init(true)
    deserializedCourse.loadRemoteInfoRecursively(project)

    if (deserializedCourse is HyperskillCourse && deserializedCourse.hyperskillProject == null) {
      deserializedCourse.reconnectHyperskillProject()
    }

    return deserializedCourse
  }

  private fun addNonEditableFilesToCourse(taskContainer: Lesson, course: Course, project: Project) {
    val nonEditableFile = taskContainer.taskList.flatMap { task ->
      task.taskFiles.values.mapNotNull { taskFile ->
        if (taskFile.isEditable) return@mapNotNull null
        project.courseDir
          .findChild(taskContainer.name)
          ?.findChild(task.name)
          ?.findFileByRelativePath(taskFile.name)
      }
    }
    project.invokeLater {
      runWriteAction {
        for (virtualFile in nonEditableFile) {
          GeneratorUtils.addNonEditableFileToCourse(course, virtualFile)
        }
      }
    }
  }

  /**
   * If project was opened with a config file containing task file that doesn't have the corresponding dir,
   * we remove it from task object but keep in the config file.
   */
  private fun Lesson.removeNonExistingTaskFiles(project: Project) {
    for (task in taskList) {
      if (this is FrameworkLesson && task.index != currentTaskIndex + 1) {
        continue
      }
      // set parent to get dir
      task.parent = this
      val taskDir = task.getDir(project.courseDir)
      val invalidTaskFilesNames = task.taskFiles
        .filter { (name, _) -> taskDir?.findFileByRelativePath(name) == null }.map { it.key }
      invalidTaskFilesNames.forEach { task.removeTaskFile(it) }
    }
  }

  @Throws(RemoteYamlLoadingException::class)
  fun Course.loadRemoteInfoRecursively(project: Project) {
    loadRemoteInfo(project)
    sections.forEach { section -> section.loadRemoteInfo(project) }

    // top-level and from sections
    visitLessons { lesson ->
      lesson.loadRemoteInfo(project)
      lesson.taskList.forEach { task -> task.loadRemoteInfo(project) }
    }
  }

  private fun HyperskillCourse.reconnectHyperskillProject() {
    LOG.info("Current project is disconnected from Hyperskill")
    val firstTask = getProjectLesson()?.taskList?.firstOrNull() ?: return
    val link = firstTask.feedbackLink ?: return
    val matchResult = HYPERSKILL_PROJECT_REGEX.matchEntire(link) ?: return
    val projectId = matchResult.groupValues[1].toInt()

    ApplicationManager.getApplication().executeOnPooledThread {
      HyperskillConnector.getInstance().getProject(projectId).let {
        when (it) {
          is Err -> return@executeOnPooledThread
          is Ok -> {
            hyperskillProject = it.value
            LOG.info("Current project successfully reconnected to Hyperskill")
          }
        }
      }

      HyperskillConnector.getInstance().getStages(projectId)?.let {
        stages = it
        LOG.info("Stages for disconnected Hyperskill project retrieved")
      }
    }
  }

  @Throws(RemoteYamlLoadingException::class)
  private fun StudyItem.loadRemoteInfo(project: Project) {
    try {
      val remoteConfigFile = remoteConfigFile(project)
      if (remoteConfigFile == null) {
        if (id > 0) {
          loadingError(
            EduCoreBundle.message("yaml.editor.invalid.format.config.file.not.found", configFileName, name)
          )
        }
        else return
      }

      loadRemoteInfo(remoteConfigFile)
    }
    catch (th: Throwable) {
      // Important: ProcessCanceledException must be propagated as-is in the IntelliJ Platform
      // and should never be wrapped. Wrapping it hides cancellation semantics and pollutes logs
      // with severe errors, like: RemoteYamlLoadingException: ProcessCanceledException.
      if (th is ProcessCanceledException) throw th
      throw RemoteYamlLoadingException(this, th)
    }
  }

  fun StudyItem.loadRemoteInfo(remoteConfigFile: VirtualFile) {
    val itemRemoteInfo = YamlDeserializer.deserializeRemoteItem(remoteConfigFile.name, VfsUtil.loadText(remoteConfigFile))
    if (itemRemoteInfo.id > 0 || itemRemoteInfo is HyperskillCourse) {
      getRemoteChangeApplierForItem(itemRemoteInfo).applyChanges(this, itemRemoteInfo)
    }
  }

  private fun Course.setDescriptionInfo(project: Project) {
    visitLessons { lesson ->
      lesson.visitTasks {
        it.updateDescriptionTextAndFormat(project)
      }
    }
  }

  private fun showYamlVersionCompatibilityNotification(project: Project, courseYamlVersion: Int, currentYamlVersion: Int) {
    val title = EduCoreBundle.message("yaml.version.compatibility.title")
    val message = EduCoreBundle.message("yaml.version.compatibility.message", courseYamlVersion, currentYamlVersion)

    com.intellij.notification.NotificationGroupManager.getInstance()
      .getNotificationGroup("Hyperskill.Academy")
      .createNotification(title, message, com.intellij.notification.NotificationType.WARNING)
      .notify(project)
  }

  /**
   * Adds all edu values to ObjectMapper needed for YAML migration.
   * If a new migration step is implemented, add here all the edu values necessary for that migration step to work.
   */
  private fun ObjectMapper.setupForMigration(project: Project) {
    setEduValue(ADDITIONAL_FILES_COLLECTOR_MAPPER_KEY) { courseType, environment, languageId ->
      val language = Language.findLanguageByID(languageId)
      if (language == null) {
        LOG.warn("Failed to find language with ID $languageId during course YAML migration: collect additional files")
        return@setEduValue emptyList()
      }
      val configurator = findConfigurator(courseType, environment, language)

      if (configurator == null) {
        LOG.warn("Failed to find EduConfigurator during course YAML migration: courseType=$courseType environment=$environment languageId=$languageId")
        return@setEduValue emptyList()
      }

      return@setEduValue collectAdditionalFiles(configurator, project, saveDocuments = false, detectTaskFoldersByContents = true)
    }
  }
}