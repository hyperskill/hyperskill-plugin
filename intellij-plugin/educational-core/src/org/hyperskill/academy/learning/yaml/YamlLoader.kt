package org.hyperskill.academy.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.messages.Topic
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.ext.customContentPath
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.getPathToChildren
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.storage.persistEduFiles
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.configFileName
import org.hyperskill.academy.learning.yaml.YamlDeserializer.childrenConfigFileNames
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.mapper
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.saveItem
import org.hyperskill.academy.learning.yaml.YamlLoader.loadItem
import org.hyperskill.academy.learning.yaml.YamlMapper.basicMapper
import org.hyperskill.academy.learning.yaml.errorHandling.*
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TASK
import org.hyperskill.academy.learning.yaml.format.getChangeApplierForItem
import org.jetbrains.annotations.NonNls

/**
 *  Get fully-initialized [StudyItem] object from yaml config file.
 *  Uses [deserializeItemProcessingErrors] to deserialize object, than applies changes to existing object, see [loadItem].
 */
object YamlLoader {
  @NonNls
  private const val TOPIC = "Loaded YAML"
  val YAML_LOAD_TOPIC: Topic<YamlListener> = Topic.create(TOPIC, YamlListener::class.java)

  fun loadItem(project: Project, configFile: VirtualFile, loadFromVFile: Boolean) {
    project.messageBus.syncPublisher(YAML_LOAD_TOPIC).beforeYamlLoad(configFile)
    try {
      doLoad(project, configFile, loadFromVFile)
    }
    catch (e: Exception) {
      when (e) {
        is YamlLoadingException -> showError(project, e, configFile, e.message)
        is RemoteYamlLoadingException -> e.processError(project)
        else -> throw e
      }
    }
  }

  @Throws(YamlLoadingException::class, RemoteYamlLoadingException::class)
  private fun doLoad(project: Project, configFile: VirtualFile, loadFromVFile: Boolean) {
    // for null course we load course again so no need to pass mode specific mapper here
    val mapper = mapper() ?: basicMapper()

    val existingItem = getStudyItemForConfig(project, configFile)
    val deserializedItem = deserializeItemProcessingErrors(configFile, project, loadFromVFile, mapper) ?: return
    val customContentPath = existingItem?.course.customContentPath
    deserializedItem.ensureChildrenExist(configFile.parent, customContentPath)

    if (existingItem == null) {
      // this code is called if item wasn't loaded because of broken config
      // and now if config fixed, we'll add item to a parent
      if (deserializedItem is Course) {
        StudyTaskManager.getInstance(project).course = YamlDeepLoader.loadCourse(project)
        return
      }

      val itemDir = configFile.parent
      deserializedItem.name = itemDir.name
      val parentItem = deserializedItem.getParentItem(project, itemDir.parent)
      val parentConfig = parentItem.getDir(project.courseDir)?.findChild(parentItem.configFileName) ?: return
      val deserializedParent = deserializeItemProcessingErrors(parentConfig, project, mapper = mapper) as? ItemContainer ?: return
      if (deserializedParent.items.map { it.name }.contains(itemDir.name)) {
        if (deserializedItem is Task) {
          deserializedItem.persistEduFiles(project)
        }
        parentItem.addItemAsNew(project, deserializedItem)
        reopenEditors(project)
        // new item is added at the end, so we should save parent item to update items order in config file
        saveItem(parentItem)
      }
      return
    }

    existingItem.applyChanges(project, deserializedItem)
    if (existingItem is Task) {
      existingItem.persistEduFiles(project)
    }
  }

  inline fun <reified T : StudyItem> StudyItem.deserializeContent(
    project: Project,
    contentList: List<T>,
    mapper: ObjectMapper = basicMapper(),
  ): List<T> {
    val content = mutableListOf<T>()
    for (titledItem in contentList) {
      val configFile: VirtualFile = getConfigFileForChild(project, titledItem.name) ?: continue
      val deserializeItem = deserializeItemProcessingErrors(configFile, project, mapper = mapper, parentItem = this) as? T ?: continue
      deserializeItem.name = titledItem.name
      deserializeItem.index = titledItem.index
      content.add(deserializeItem)
    }

    return content
  }

  fun StudyItem.getConfigFileForChild(project: Project, childName: String): VirtualFile? {
    val courseDir = project.courseDir
    val dir = getDir(courseDir) ?: error(noDirForItemMessage(name))
    val itemDir = dir.findFileByRelativePath(getPathToChildren())?.findChild(childName)

    val configFile = childrenConfigFileNames.map { itemDir?.findChild(it) }.firstOrNull { it != null }
    if (configFile != null) {
      return configFile
    }

    val message = if (itemDir == null) {
      EduCoreBundle.message("yaml.editor.notification.directory.not.found", childName)
    }
    else {
      EduCoreBundle.message("yaml.editor.notification.config.file.not.found", childName)
    }

    @NonNls
    val errorMessageToLog = "Config file for currently loading item $name not found"
    val parentConfig = dir.findChild(configFileName) ?: error(errorMessageToLog)
    showError(project, null, parentConfig, message)

    return null
  }

  /**
   * For items that are added as new we have to reopen editors, because `EduSplitEditor` wasn't created
   * for files that aren't task files.
   */
  private fun reopenEditors(project: Project) {
    val selectedEditor = FileEditorManager.getInstance(project).selectedEditor
    val files = FileEditorManager.getInstance(project).openFiles
      .filter { it.getTaskFile(project) != null }
    for (virtualFile in files) {
      FileEditorManager.getInstance(project).closeFile(virtualFile)
      FileEditorManager.getInstance(project).openFile(virtualFile, false)
    }

    // restore selection
    val file = selectedEditor?.file
    if (file != null) {
      FileEditorManager.getInstance(project).openFile(file, true)
    }
  }

  fun ItemContainer.addItemAsNew(project: Project, deserializedItem: StudyItem) {
    addItem(deserializedItem)
    sortItems()
    // we need parent to be set to obtain directories for children config files
    deserializedItem.parent = this
    deserializedItem.deserializeChildrenIfNeeded(project, course)
  }

  fun StudyItem.deserializeChildrenIfNeeded(project: Project, course: Course) {
    if (this !is ItemContainer) {
      return
    }

    val mapper = mapper()
    items = deserializeContent(project, items, mapper)
    // set parent to deserialize content correctly
    items.forEach { it.init(this, false) }
    items.filterIsInstance<ItemContainer>().forEach { item ->
      item.items = item.deserializeContent(project, item.items, mapper)
      item.items.forEach { it.init(item, false) }
    }
  }

  private fun StudyItem.getParentItem(project: Project, parentDir: VirtualFile): ItemContainer {
    val course = StudyTaskManager.getInstance(project).course
    val customContentPath = course.customContentPath
    val itemContainer = when (this) {
      is Section -> if (project.courseDir.findFileByRelativePath(customContentPath) == parentDir) course else null
      is Lesson -> if (project.courseDir.findFileByRelativePath(customContentPath) == parentDir) {
        course
      }
      else {
        course?.let { parentDir.getSection(project) }
      }

      is Task -> course?.let { parentDir.getLesson(project) }
      else -> loadingError(
        EduCoreBundle.message("yaml.editor.invalid.unexpected.item.type", itemType)
      )
    }
    return itemContainer ?: loadingError(EduCoreBundle.message("yaml.editor.invalid.format.parent.not.found", name))
  }

  private fun <T : StudyItem> T.applyChanges(project: Project, deserializedItem: T) {
    getChangeApplierForItem(project, deserializedItem).applyChanges(this, deserializedItem)
  }

  private fun getStudyItemForConfig(project: Project, configFile: VirtualFile): StudyItem? {
    val name = configFile.name
    val itemDir = configFile.parent ?: error(EduCoreBundle.message("yaml.editor.invalid.format.containing.item.dir.not.found", name))
    val course = StudyTaskManager.getInstance(project).course ?: return null
    return when (name) {
      YamlConfigSettings.COURSE_CONFIG -> course
      YamlConfigSettings.SECTION_CONFIG -> itemDir.getSection(project)
      YamlConfigSettings.LESSON_CONFIG -> itemDir.getLesson(project)
      YamlConfigSettings.TASK_CONFIG -> itemDir.getTask(project)
      else -> loadingError(unknownConfigMessage(name))
    }
  }

  @VisibleForTesting
  class ProcessedException(message: String, originalException: Exception?) : Exception(message, originalException)

}

private fun StudyItem.ensureChildrenExist(itemDir: VirtualFile, customContentPath: String) {
  when (this) {
    is ItemContainer -> {
      items.forEach {
        val itemTypeName = if (it is Task) TASK else EduNames.ITEM
        itemDir.findFileByRelativePath(this.getPathToChildren(customContentPath))?.findChild(it.name) ?: loadingError(
          noDirForItemMessage(it.name, itemTypeName)
        )
      }
    }

    is Task -> {
      taskFiles.forEach { (name, _) ->
        itemDir.findFileByRelativePath(name) ?: loadingError(EduCoreBundle.message("yaml.editor.invalid.format.no.file", name))
      }
    }
  }
}
