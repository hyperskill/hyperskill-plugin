package org.hyperskill.academy.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.RightAlignedToolbarAction
import com.intellij.openapi.project.DumbAwareAction
import org.hyperskill.academy.learning.EduBrowser
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.jetbrains.annotations.NonNls

class LeaveFeedbackAction :
  DumbAwareAction(EduCoreBundle.lazyMessage("action.leave.comment.text"), EduCoreBundle.lazyMessage("action.leave.comment.text")),
  RightAlignedToolbarAction {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    val link = task.feedbackLink ?: error("LeaveFeedbackAction is not supported")
    EduBrowser.getInstance().browse(link)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = false

    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    val course = task.course
    if (course is HyperskillCourse) {
      e.presentation.text = EduCoreBundle.message("action.show.discussions.text")
      e.presentation.description = EduCoreBundle.message("action.show.discussions.description")
      addSynonym(EduCoreBundle.lazyMessage("action.show.discussions.text"))
    }

    e.presentation.isEnabledAndVisible = task.feedbackLink != null
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  companion object {
    @NonNls
    const val ACTION_ID: String = "HyperskillEducational.LeaveFeedbackAction"
  }
}
