package org.hyperskill.academy.learning.taskToolWindow

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.getContainingTask
import org.hyperskill.academy.learning.getTaskFile
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowFactory
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView

class EduFileEditorManagerListener(private val project: Project) : FileEditorManagerListener, FileEditorManagerListener.Before {

  override fun beforeFileOpened(source: FileEditorManager, file: VirtualFile) {
    if (!project.isEduProject()) return
    val taskFile = file.getTaskFile(project) ?: return
    showTaskDescriptionToolWindow(project, taskFile, true)
  }

  override fun selectionChanged(event: FileEditorManagerEvent) {
    if (!project.isEduProject()) return
    val file = event.newFile
    val task = file?.getContainingTask(project) ?: return
    TaskToolWindowView.getInstance(project).currentTask = task
  }

  override fun fileClosed(source: FileEditorManager, file: VirtualFile) {
    if (project.isEduProject() && FileEditorManager.getInstance(project).openFiles.isEmpty()) {
      TaskToolWindowView.getInstance(project).currentTask = null
    }
  }

  private fun showTaskDescriptionToolWindow(project: Project, taskFile: TaskFile, retry: Boolean) {
    val toolWindowManager = ToolWindowManager.getInstance(project)
    val studyToolWindow = toolWindowManager.getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW)
    if (studyToolWindow == null) {
      if (retry) {
        toolWindowManager.invokeLater { showTaskDescriptionToolWindow(project, taskFile, false) }
      }
      else {
        LOG.warn(String.format("Failed to get toolwindow with `%s` id", TaskToolWindowFactory.STUDY_TOOL_WINDOW))
      }
      return
    }
    if (taskFile.task != TaskToolWindowView.getInstance(project).currentTask) {
      EduUtilsKt.updateToolWindows(project)
      studyToolWindow.show(null)
    }
  }

  companion object {
    private val LOG = logger<EduFileEditorManagerListener>()
  }
}