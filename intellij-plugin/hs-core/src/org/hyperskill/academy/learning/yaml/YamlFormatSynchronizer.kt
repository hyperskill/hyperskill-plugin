package org.hyperskill.academy.learning.yaml

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderEx
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ItemContainer
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.ext.disambiguateContents
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.ext.project
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.storage.LearningObjectsStorageManager
import org.hyperskill.academy.learning.storage.persistAdditionalFiles
import org.hyperskill.academy.learning.storage.persistEduFiles
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_LESSON_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.TASK_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.configFileName
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.remoteConfigFileName
import org.hyperskill.academy.learning.yaml.YamlMapper.basicMapper
import org.hyperskill.academy.learning.yaml.YamlMapper.remoteMapper
import org.jetbrains.annotations.NonNls
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel

object YamlFormatSynchronizer {
  private val LOG = Logger.getInstance(YamlFormatSynchronizer::class.java)

  val LOAD_FROM_CONFIG = Key<Boolean>("Hyperskill.loadItem")
  val SAVE_TO_CONFIG = Key<Boolean>("Hyperskill.saveItem")
  private val SYNCHRONIZATION_STARTED = Key<Boolean>("Hyperskill.synchronizationStarted")

  fun saveAll(project: Project) {
    // If there is no course associated with the project, there is nothing to save.
    // This can happen for regular projects or welcome/preview projects.
    val course = StudyTaskManager.getInstance(project).course ?: return
    // Respect settings: do not attempt to create config files if this feature is disabled for the project.
    if (!YamlFormatSettings.shouldCreateConfigFiles(project)) return
    // If course directory was deleted or is invalid, skip saving to avoid InvalidVirtualFileAccessException.
    val courseDir = project.guessCourseDir()
    if (courseDir == null || !courseDir.isValid) return
    val mapper = mapper()
    saveItem(course, mapper)
    course.visitSections { section -> saveItem(section, mapper) }
    course.visitLessons { lesson ->
      lesson.visitTasks { task ->
        saveItem(task, mapper)
      }
      saveItem(lesson, mapper)
    }

    saveRemoteInfo(course)
  }

  fun saveItem(item: StudyItem, mapper: ObjectMapper = mapper(), configName: String = item.configFileName) {
    val course = item.course

    @NonNls
    val errorMessageToLog = "Failed to find project for course"
    val project = course.project ?: error(errorMessageToLog)
    if (!YamlFormatSettings.shouldCreateConfigFiles(project)) {
      return
    }
    // The item holds fewer children than its config file on disk claims, because some of them could not be resolved
    // during the last load. Writing it out now would persist the truncated `content:` list and permanently drop the
    // missing children from the course. Skipping the save keeps the on-disk structure intact until a load succeeds.
    //
    // Only the structural config is guarded. `*-remote-info.yaml` carries no `content:` list, so a partially loaded
    // item has to keep persisting its remote state (id, update date, submissions) as usual -- otherwise a single
    // unresolvable child would silently freeze the remote state of its whole container for the rest of the session.
    if (item.isPartiallyLoaded && configName == item.configFileName) {
      LOG.warn("Skipping save of ${item.itemType} `${item.name}`: its children were not fully resolved during load")
      return
    }

    item.saveConfig(project, configName, mapper)
  }

  fun saveRemoteInfo(item: StudyItem) {
    when (item) {
      is ItemContainer -> {
        saveItemRemoteInfo(item)
        item.items.forEach { saveRemoteInfo(it) }
      }

      is Task -> {
        saveItemRemoteInfo(item)
      }
    }
  }

  fun saveItemWithRemoteInfo(item: StudyItem) {
    saveItem(item)
    saveRemoteInfo(item)
  }

  private fun saveItemRemoteInfo(item: StudyItem) {
    // we don't want to create remote info files in local courses
    if (item.id > 0 || item is HyperskillCourse) {
      saveItem(item, remoteMapper(), item.remoteConfigFileName)
    }
  }

  fun startSynchronization(project: Project) {
    if (isUnitTestMode) {
      return
    }

    // `StudyTaskManager.initializeCourse` may call this more than once for the same project now that a load failure
    // is no longer latched forever. Without this guard every extra call would register another global document
    // listener and another message bus connection, multiplying the notifications each YAML edit produces.
    if (!(project as UserDataHolderEx).replace(SYNCHRONIZATION_STARTED, null, true)) return

    val disposable = StudyTaskManager.getInstance(project)
    EditorFactory.getInstance().eventMulticaster.addDocumentListener(YamlSynchronizationListener(project), disposable)
    project.messageBus.connect().subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (isLocalConfigFile(file)) {
          @NonNls
          val errorMessageToLog = "Can't find editor for a file: ${file.name}"
          val editor = file.getEditor(project) ?: error(errorMessageToLog)
          showNoEditingNotification(editor)
          return
        }
      }
    })
  }

  private fun showNoEditingNotification(editor: Editor) {
    val label = JLabel(EduCoreBundle.message("yaml.editor.notification.configuration.file"))
    label.border = JBUI.Borders.empty(5, 10, 5, 0)

    val panel = JPanel(BorderLayout())
    panel.add(label, BorderLayout.CENTER)
    panel.background = MessageType.WARNING.popupBackground

    editor.headerComponent = panel
  }

  private fun StudyItem.saveConfig(project: Project, configName: String, mapper: ObjectMapper) {
    val dir = getConfigDir(project) ?: return

    val configFile = runReadAction { dir.findChild(configName) }
    if (configFile?.getUserData(SAVE_TO_CONFIG) == false) return

    if (this is Task) {
      disambiguateTaskFilesContents(project)
      persistEduFiles(project)
    }

    if (this is Course) {
      disambiguateAdditionalFilesContents(project)
      persistAdditionalFiles(project)
    }

    // Wait for all persistence operations to complete before YAML serialization
    // to avoid race conditions with concurrent access to FileContents
    LearningObjectsStorageManager.getInstance(project).waitForPersistingTasks()

    project.invokeLater {
      runWriteAction {
        val file = dir.findOrCreateChildData(javaClass, configName)
        try {
          file.putUserData(LOAD_FROM_CONFIG, false)
          if (FileTypeManager.getInstance().getFileTypeByFile(file) == UnknownFileType.INSTANCE) {
            @NonNls
            val errorMessageToLog = "Failed to get extension for file ${file.name}"
            FileTypeManager.getInstance().associateExtension(
              PlainTextFileType.INSTANCE,
              file.extension ?: error(errorMessageToLog)
            )
          }
          val yamlText = mapper.writeValueAsString(this)
          val formattedYamlText = reformatYaml(project, file.name, yamlText)

          VfsUtil.saveText(file, formattedYamlText)
          // make sure that there is no conflict between disk contents and ide in-memory document contents
          FileDocumentManager.getInstance().reloadFiles(file)
        }
        finally {
          file.putUserData(LOAD_FROM_CONFIG, true)
        }
      }
    }
  }

  private fun reformatYaml(project: Project, fileName: String, text: String): String {
    // We are able to reformat YAML only if the IDE supports the YAML language
    val yamlFileType = FileTypeManager.getInstance().findFileTypeByName("YAML") ?: return text

    val psiFile = PsiFileFactory.getInstance(project).createFileFromText(fileName, yamlFileType, text)
    CodeStyleManager.getInstance(project).reformat(psiFile)

    return psiFile.text ?: text
  }

  fun isConfigFile(file: VirtualFile): Boolean {
    return isLocalConfigFile(file) || isRemoteConfigFile(file)
  }

  fun isRemoteConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return isRemoteConfigFileName(name)
  }

  fun isRemoteConfigFileName(name: String): Boolean {
    return REMOTE_COURSE_CONFIG == name || REMOTE_SECTION_CONFIG == name || REMOTE_LESSON_CONFIG == name || REMOTE_TASK_CONFIG == name
  }

  fun isLocalConfigFile(file: VirtualFile): Boolean {
    val name = file.name
    return isLocalConfigFileName(name)
  }

  fun isLocalConfigFileName(name: String): Boolean {
    return COURSE_CONFIG == name || SECTION_CONFIG == name || LESSON_CONFIG == name || TASK_CONFIG == name
  }

  fun mapper(): ObjectMapper = basicMapper()
}

private fun Task.disambiguateTaskFilesContents(project: Project) {
  for ((path, taskFile) in taskFiles) {
    val file = taskFile.getVirtualFile(project)
    val disambiguatedContents = if (file != null) {
      taskFile.contents.disambiguateContents(file)
    }
    else {
      taskFile.contents.disambiguateContents(path)
    }

    taskFile.contents = disambiguatedContents
  }
}

private fun Course.disambiguateAdditionalFilesContents(project: Project) {
  val courseDir = project.courseDir

  additionalFiles.map { additionalFile ->
    val filePath = additionalFile.name
    val file = courseDir.findFile(filePath)

    val disambiguatedContents = if (file != null) {
      additionalFile.contents.disambiguateContents(file)
    }
    else {
      additionalFile.contents.disambiguateContents(filePath)
    }

    additionalFile.contents = disambiguatedContents
  }
}
