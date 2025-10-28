package org.hyperskill.academy.ai.debugger.core.ui

import com.intellij.openapi.project.Project
import org.hyperskill.academy.ai.debugger.core.feedback.AIDebugContext
import org.hyperskill.academy.ai.debugger.core.feedback.AIDebuggerFeedbackDialog
import org.hyperskill.academy.ai.debugger.core.log.AIDebuggerLogEntry
import org.hyperskill.academy.ai.debugger.core.log.logInfo
import org.hyperskill.academy.ai.debugger.core.log.toTaskData
import org.hyperskill.academy.learning.courseFormat.ext.project
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.ui.HintInlineBanner
import org.hyperskill.academy.learning.ui.LikeBlock
import org.jetbrains.annotations.Nls

// TODO: drop it
class AIDebuggerHintInlineBanner(
  project: Project,
  task: Task,
  message: @Nls String,
) : HintInlineBanner(project, task, message) {

  fun addFeedbackLikenessButtons(
    task: Task,
    debugContext: AIDebugContext
  ): AIDebuggerHintInlineBanner {
    val project = task.project ?: return this
    addLikeDislikeActions {
      val dialog = AIDebuggerFeedbackDialog(project, debugContext, likeness)
      val likenessAnswer = if (dialog.showAndGet()) {
        dialog.getLikenessAnswer() ?: likeness
      } else {
        LikeBlock.FeedbackLikenessAnswer.NO_ANSWER
      }
      AIDebuggerLogEntry(
        task = task.toTaskData(),
        actionType = "DebugSessionStopped",
        feedbackLikenessAnswer = likenessAnswer,
        feedbackText = dialog.getExperienceText()
      ).logInfo()
      close()
      likenessAnswer
    }
    return this
  }
}
