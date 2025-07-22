package org.hyperskill.academy.learning.actions

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.ui.EditorNotifications
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.actions.EduActionUtils.updateAction
import org.hyperskill.academy.learning.courseFormat.ext.revertTaskFiles
import org.hyperskill.academy.learning.courseFormat.ext.revertTaskParameters
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.projectView.ProgressUtil.updateCourseProgress
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.VisibleForTesting

class RevertTaskAction : DumbAwareAction(), RightAlignedToolbarAction {
  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    if (task.isChangedOnFailed) {
      return
    }
    val result = MessageDialogBuilder.yesNo(
      EduCoreBundle.message("action.Educational.RefreshTask.text"),
      EduCoreBundle.message("action.Educational.RefreshTask.progress.dropped")
    ).ask(project)
    if (!result) return
    revert(project, task)
  }

  override fun update(e: AnActionEvent) {
    updateAction(e)
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    if (
      !task.course.isStudy ||
      task.isChangedOnFailed // we disable revert action for tasks with changing on error
    ) {
      val presentation = e.presentation
      presentation.isEnabledAndVisible = false
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    @NonNls
    const val ACTION_ID: String = "HyperskillEducational.RefreshTask"

    val EP_NAME: ExtensionPointName<RevertTaskExtension> = ExtensionPointName.create("HyperskillEducational.revertTaskExtension")

    @VisibleForTesting
    fun revert(project: Project, task: Task) {
      task.apply {
        revertTaskFiles(project)
        revertTaskParameters()
        YamlFormatSynchronizer.saveItem(this)
      }

      EP_NAME.forEachExtensionSafe {
        it.onTaskReversion(project, task)
      }

      EditorNotifications.getInstance(project).updateAllNotifications()
      EduNotificationManager.showInfoNotification(project, content = EduCoreBundle.message("action.Educational.RefreshTask.result"))
      ProjectView.getInstance(project).refresh()
      TaskToolWindowView.getInstance(project).updateTaskSpecificPanel()
      TaskToolWindowView.getInstance(project).readyToCheck()
      updateCourseProgress(project)
    }
  }

  fun interface RevertTaskExtension {
    fun onTaskReversion(project: Project, task: Task)
  }
}