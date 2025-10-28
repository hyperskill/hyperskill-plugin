package org.hyperskill.academy.ai.debugger.core.feedback

import com.intellij.openapi.project.Project
import com.intellij.platform.feedback.dialog.BlockBasedFeedbackDialog
import com.intellij.platform.feedback.dialog.CommonFeedbackSystemData
import com.intellij.platform.feedback.dialog.showFeedbackSystemInfoDialog
import com.intellij.platform.feedback.dialog.uiBlocks.FeedbackBlock
import com.intellij.platform.feedback.dialog.uiBlocks.TextAreaBlock
import org.hyperskill.academy.ai.debugger.core.messages.AIDebuggerCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.ui.LikeBlock
import org.hyperskill.academy.learning.ui.LikeBlock.FeedbackLikenessAnswer

class AIDebuggerFeedbackDialog(
  private val project: Project,
  private val debugContext: AIDebugContext,
  defaultLikeness: FeedbackLikenessAnswer = FeedbackLikenessAnswer.NO_ANSWER
) : BlockBasedFeedbackDialog<AIDebuggerFeedbackCommonInfoData>(project, false) {

  override val myFeedbackReportId: String = "edu_ai_debugger_feedback"

  override val myShowFeedbackSystemInfoDialog: () -> Unit = {
    showFeedbackSystemInfoDialog(project, mySystemInfoData.commonSystemInfo) {
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.dialog.system.info.course.id")) {
        label(mySystemInfoData.courseId.toString())
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.dialog.system.info.course.name")) {
        label(mySystemInfoData.courseName)
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.dialog.system.info.task.id")) {
        label(mySystemInfoData.taskId.toString())
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.dialog.system.info.task.name")) {
        label(mySystemInfoData.taskName)
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.label.student.solution")) {
        label(mySystemInfoData.studentSolution.toString())
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.label.test.name")) {
        label(mySystemInfoData.testName)
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.label.test.error.message")) {
        label(mySystemInfoData.testErrorMessage)
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.label.test.expected.output")) {
        label(mySystemInfoData.testExpectedOutput)
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.label.test.text")) {
        label(mySystemInfoData.testText)
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.label.final.breakpoints")) {
        label(mySystemInfoData.finalBreakpoints.toString())
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.label.intermediate.breakpoints")) {
        label(mySystemInfoData.intermediateBreakpoints.toString())
      }
      row(AIDebuggerCoreBundle.message("ai.debugger.feedback.label.breakpoint.hints")) {
        label(mySystemInfoData.breakpointHints.toString())
      }
    }
  }

  override val mySystemInfoData: AIDebuggerFeedbackCommonInfoData by lazy {
    AIDebuggerFeedbackCommonInfoData.create(
      commonSystemInfo = CommonFeedbackSystemData.getCurrentData(),
      debugContext = debugContext
    )
  }

  override val myTitle: String
    get() = AIDebuggerCoreBundle.message("ai.debugger.feedback.dialog.title")

  override val myBlocks: List<FeedbackBlock> = listOf(
    LikeBlock(
      AIDebuggerCoreBundle.message("ai.debugger.feedback.like.label"),
      "hints_likeness",
      defaultLikeness
    ),
    TextAreaBlock("", "hints_experience")
      .setPlaceholder(AIDebuggerCoreBundle.message ("ai.debugger.feedback.dialog.textarea.optional.label"))
  )

  fun getLikenessAnswer(): FeedbackLikenessAnswer? = (myBlocks.firstOrNull() as? LikeBlock)?.answer

  fun getExperienceText(): String {
    val result = StringBuilder()
    myBlocks.filterIsInstance<TextAreaBlock>().firstOrNull()?.collectBlockTextDescription(result)
    return result.toString().trim()
  }

  override fun showThanksNotification() {
    EduNotificationManager.showInfoNotification(
      project = project,
      title = AIDebuggerCoreBundle.message("ai.debugger.feedback.notification.title"),
      content = AIDebuggerCoreBundle.message("ai.debugger.feedback.notification.text")
    )
  }

  init {
    init()
  }
}