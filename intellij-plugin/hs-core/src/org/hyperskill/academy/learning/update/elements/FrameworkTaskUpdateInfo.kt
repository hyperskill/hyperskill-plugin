package org.hyperskill.academy.learning.update.elements

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.ReadOnlyAttributeUtil
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.FileContents
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.getTaskDirectory
import org.hyperskill.academy.learning.courseFormat.ext.shouldBePropagated
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.hyperskill.academy.learning.toCourseInfoHolder
import org.hyperskill.academy.learning.update.FrameworkLessonHistory
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

data class FrameworkTaskUpdateInfo(
  override val localItem: Task,
  override val remoteItem: Task,
  private val taskHistory: FrameworkLessonHistory
) : TaskUpdate(localItem, remoteItem) {

  override suspend fun update(project: Project) {
    val flManager = FrameworkLessonManager.getInstance(project)

    val localLesson = localItem.parent as FrameworkLesson

    val taskIsCurrent = localLesson.currentTaskIndex == localItem.index - 1

    if (taskIsCurrent) {
      // The learner's most recent edits on the current stage may live only in in-memory editor
      // documents and not yet be flushed to disk. Persist them before reading local text and
      // overwriting files, so that preserved learner changes survive the later VFS refresh
      // (e.g. in `updateFile`/`reloadFiles` or when the project is reopened). This mirrors the
      // document flush performed during framework lesson navigation.
      withContext(Dispatchers.EDT) {
        FileDocumentManager.getInstance().saveAllDocuments()
      }
    }

    if (localItem.status != CheckStatus.Solved) {
      remoteItem.status = CheckStatus.Unchecked
    }
    else {
      remoteItem.status = CheckStatus.Solved
    }

    try {
      updateTaskDirectory(project, localLesson)
    }
    catch (e: Exception) {
      thisLogger().error("Failed to update task directory", e)
      return
    }

    remoteItem.record = localItem.record
    flManager.updateUserChanges(
      localItem,
      remoteItem.taskFiles.mapValues { it.value.contents.textualRepresentation },
      remoteItem.taskFiles
    )
    val initialTaskFileContents = localItem.taskFiles.mapValues { (_, taskFile) ->
      taskFile.contents.textualRepresentation
    }
    val localTaskFiles = localItem.taskFiles.mapValues { (_, taskFile) -> taskFile.copy() }
    localLesson.replaceItem(localItem, remoteItem)
    remoteItem.init(localLesson, false)

    if (taskIsCurrent) {
      val taskDir = remoteItem.getDir(project.courseDir) ?: return

      for ((fileName, fileHistory) in taskHistory.taskFileHistories) {
        val fileContents = fileHistory.evaluateContents(localLesson.currentTaskIndex)
        val localUnmodifiedContents = fileHistory.evaluateLocalUnmodifiedContents(localLesson.currentTaskIndex)
        val localText = readLocalText(taskDir, fileName)
        if (fileContents == null) {
          if (hasLocalChanges(fileName, localText, localUnmodifiedContents, initialTaskFileContents)) {
            thisLogger().info("Preserved local changes for '$fileName', skipping remote deletion")
            preserveLocalTaskFile(fileName, localText, localTaskFiles)
            continue
          }
          remoteItem.removeTaskFile(fileName)
          removeFile(taskDir, fileName)
        }
        else {
          val taskFile = remoteItem.taskFiles[fileName]
          if (taskFile?.shouldBePropagated() != false &&
              hasLocalChanges(fileName, localText, localUnmodifiedContents, initialTaskFileContents)) {
            // Keep the learner's edits on disk (they are already flushed there) by skipping the
            // remote overwrite.
            if (taskFile != null) {
              // The file still exists remotely: keep the author's updated contents in the task
              // model (already set by the preceding replaceItem/remoteItem.init) so that a later
              // revert restores the author version. Only the learner's file on disk is kept.
              thisLogger().info("Preserved local changes for '$fileName' on disk, keeping author contents in model")
            }
            else {
              // The file no longer exists remotely: preserve it in the model so it stays tracked.
              thisLogger().info("Preserved local changes for '$fileName', skipping remote update")
              preserveLocalTaskFile(fileName, localText, localTaskFiles)
            }
            continue
          }
          val isEditable = taskFile?.isEditable != false
          updateFile(project, taskDir, fileName, fileContents, isEditable)
          remoteItem.taskFiles[fileName]?.contents = fileContents
        }
      }
    }

    flManager.updateOriginalTestFiles(remoteItem)
    flManager.updateOriginalTemplateFiles(remoteItem)
    flManager.updateSnapshotTestFiles(remoteItem)

    YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
  }

  @Throws(IOException::class, IllegalStateException::class)
  private suspend fun updateTaskDirectory(project: Project, localLesson: FrameworkLesson) {
    val newTaskDir = if (localItem.name != remoteItem.name) {
      createTaskDirectoryWithNewName(project, localLesson)
    }
    else {
      localItem.getTaskDirectory(project) ?: error("Failed to find local task dir: ${localLesson.name}")
    }

    GeneratorUtils.createDescriptionFile(project, newTaskDir, remoteItem)
  }

  @Throws(IOException::class, IllegalStateException::class)
  private suspend fun createTaskDirectoryWithNewName(project: Project, localLesson: FrameworkLesson): VirtualFile {
    val localTaskDir = localItem.getTaskDirectory(project) ?: error("Failed to find local task dir on update: ${localLesson.name}")

    writeAction {
      localTaskDir.delete(FrameworkTaskUpdateInfo::class)
    }

    val lessonDir = localLesson.getDir(project.courseDir) ?: error("Failed to find local lesson dir on update: ${localLesson.name}")

    val newTaskDir = writeAction {
      VfsUtil.createDirectoryIfMissing(lessonDir, remoteItem.name)
    }

    return newTaskDir
  }

  private suspend fun removeFile(taskDir: VirtualFile, fileName: String) = writeAction {
    val virtualChangedFile = taskDir.findFileByRelativePath(fileName)
    virtualChangedFile?.delete(this)
  }

  private suspend fun readLocalText(taskDir: VirtualFile, fileName: String): String? {
    val virtualFile = readAction {
      taskDir.findFileByRelativePath(fileName)
    } ?: return null

    return readAction {
      FileDocumentManager.getInstance().getDocument(virtualFile)?.text
    }
  }

  private fun hasLocalChanges(
    fileName: String,
    localText: String?,
    localUnmodifiedContents: FileContents?,
    initialTaskFileContents: Map<String, String>
  ): Boolean {
    if (localText == null) return false
    val initialText = localUnmodifiedContents?.textualRepresentation
      ?: initialTaskFileContents[fileName]
    return initialText == null || localText != initialText
  }

  private fun preserveLocalTaskFile(fileName: String, localText: String?, localTaskFiles: Map<String, TaskFile>) {
    val localTaskFile = localTaskFiles[fileName] ?: return
    val preservedContents = localText?.let(::InMemoryTextualContents) ?: localTaskFile.contents
    val remoteTaskFile = remoteItem.taskFiles[fileName]
    if (remoteTaskFile == null) {
      remoteItem.addTaskFile(localTaskFile.copy(preservedContents))
    }
    else {
      remoteTaskFile.contents = preservedContents
    }
  }

  private fun TaskFile.copy(contents: FileContents = this.contents): TaskFile {
    return TaskFile(name, contents).also { copy ->
      copy.isVisible = isVisible
      copy.isEditable = isEditable
      copy.isPropagatable = isPropagatable
      copy.isLearnerCreated = isLearnerCreated
      copy.isTrackChanges = isTrackChanges
      copy.errorHighlightLevel = errorHighlightLevel
      copy.additionalProperties = additionalProperties.toMutableMap()
    }
  }

  private suspend fun updateFile(project: Project, taskDir: VirtualFile, fileName: String, contents: FileContents, isEditable: Boolean) {

    val virtualChangedFile = readAction {
      taskDir.findFileByRelativePath(fileName)
    }

    if (virtualChangedFile != null) {
      withContext(Dispatchers.EDT) {
        FileDocumentManager.getInstance().reloadFiles(virtualChangedFile)
      }
      writeAction {
        ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualChangedFile, false)
      }
    }

    GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, fileName, contents, isEditable)
  }
}
