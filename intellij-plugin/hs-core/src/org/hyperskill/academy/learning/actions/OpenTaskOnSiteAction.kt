package org.hyperskill.academy.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import org.hyperskill.academy.learning.EduBrowser
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillTaskLink
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.NonNls


class OpenTaskOnSiteAction : DumbAwareAction(EduCoreBundle.lazyMessage("action.open.on.site.text")), RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = TaskToolWindowView.getInstance(project).currentTask ?: return
    val link = if (task.course is HyperskillCourse) hyperskillTaskLink(task) else return
    EduBrowser.getInstance().browse(link)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    val course = task.course

    e.presentation.isEnabledAndVisible = course is HyperskillCourse
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  companion object {
    @NonNls
    const val ACTION_ID: String = "HyperskillEducational.OpenTaskOnSiteAction"
  }
}