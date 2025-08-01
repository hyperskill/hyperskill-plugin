package org.hyperskill.academy.learning.update.elements

import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.ReadOnlyAttributeUtil
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.FileContents
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.getTaskDirectory
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.hyperskill.academy.learning.toCourseInfoHolder
import org.hyperskill.academy.learning.update.FrameworkLessonHistory
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
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
    flManager.updateUserChanges(localItem, remoteItem.taskFiles.mapValues { it.value.contents.textualRepresentation })
    localLesson.replaceItem(localItem, remoteItem)
    remoteItem.init(localLesson, false)

    if (taskIsCurrent) {
      val taskDir = remoteItem.getDir(project.courseDir) ?: return

      for ((fileName, fileHistory) in taskHistory.taskFileHistories) {
        val fileContents = fileHistory.evaluateContents(localLesson.currentTaskIndex)
        if (fileContents == null) {
          removeFile(taskDir, fileName)
        }
        else {
          val isEditable = remoteItem.taskFiles[fileName]?.isEditable != false
          updateFile(project, taskDir, fileName, fileContents, isEditable)
        }
      }
    }

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }

  @Throws(IOException::class, IllegalStateException::class)
  private suspend fun updateTaskDirectory(project: Project, localLesson: FrameworkLesson) {
    val newTaskDir = if (localItem.name != remoteItem.name) {
      createTaskDirectoryWithNewName(project, localLesson)
    }
    else {
      localItem.getTaskDirectory(project) ?: error("Failed to find local task dir: ${localLesson.name}")
    }

    blockingContext {
      GeneratorUtils.createDescriptionFile(project, newTaskDir, remoteItem)
    }
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

  private suspend fun updateFile(project: Project, taskDir: VirtualFile, fileName: String, contents: FileContents, isEditable: Boolean) {

    val virtualChangedFile = readAction {
      taskDir.findFileByRelativePath(fileName)
    }

    if (virtualChangedFile != null) {
      writeAction {
        FileDocumentManager.getInstance().reloadFiles(virtualChangedFile)
        ReadOnlyAttributeUtil.setReadOnlyAttribute(virtualChangedFile, false)
      }
    }

    blockingContext {
      GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, fileName, contents, isEditable)
    }
  }
}