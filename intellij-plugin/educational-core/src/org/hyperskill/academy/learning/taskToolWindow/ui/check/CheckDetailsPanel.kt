package org.hyperskill.academy.learning.taskToolWindow.ui.check

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.requests.SimpleDiffRequest
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.components.AnActionLink
import com.intellij.ui.content.Content
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.EduUtilsKt.isStudentProject
import org.hyperskill.academy.learning.actions.CompareWithAnswerAction
import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.checker.details.CheckDetailsView
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckResultDiff
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.ext.canShowSolution
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.hyperskill.PostHyperskillProjectToGithub
import org.hyperskill.academy.learning.taskToolWindow.addActionLinks
import org.hyperskill.academy.learning.taskToolWindow.ui.LightColoredActionLink
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowFactory
import org.hyperskill.academy.learning.taskToolWindow.ui.check.CheckMessagePanel.Companion.FOCUS_BORDER_WIDTH
import org.hyperskill.academy.learning.xmlUnescaped
import java.awt.BorderLayout
import javax.swing.BoxLayout
import javax.swing.JPanel

class CheckDetailsPanel(project: Project, task: Task, checkResult: CheckResult, alarm: Alarm) : JPanel(BorderLayout()) {
  init {
    val feedbackPanel = CheckFeedbackPanel(task, checkResult, alarm)
    val linksPanel = createLinksPanel(project, checkResult, task)
    val messagePanel = createMessagePanel(project, checkResult, linksPanel)

    if (feedbackPanel.isVisible) {
      add(feedbackPanel, BorderLayout.NORTH)
    }
    if (messagePanel.isVisible) {
      add(messagePanel, BorderLayout.CENTER)
    }
    // TODO rewrite this piece: create separate LinksPanel class, try to get rid of dependencies in messagePanel
    if (linksPanel.componentCount > 0) {
      add(linksPanel, BorderLayout.SOUTH)
    }
  }

  override fun isVisible(): Boolean = componentCount > 0

  private fun createMessagePanel(project: Project, checkResult: CheckResult, linksPanel: JPanel): CheckMessagePanel {
    val messagePanel = CheckMessagePanel.create(checkResult)
    val details = checkResult.details
    if (details != null && checkResult.message in CheckUtils.ERRORS) {
      CheckDetailsView.getInstance(project).showCheckResultDetails(checkResult.message, details)
    }

    if (messagePanel.messageShortened || details != null) {
      linksPanel.add(ShowFullOutputAction(project, checkResult.fullMessage.xmlUnescaped).actionLink, BorderLayout.CENTER)
    }
    return messagePanel
  }

  private fun createLinksPanel(project: Project, checkResult: CheckResult, task: Task): JPanel {
    val linksPanel = JPanel(BorderLayout())
    linksPanel.border = JBUI.Borders.emptyLeft(FOCUS_BORDER_WIDTH)

    val course = task.course

    addActionLinks(course, linksPanel, 16, 0)

    if (course is HyperskillCourse) {
      if (course.isTaskInProject(task) && checkResult.status == CheckStatus.Failed) {
        val showMoreInfo = LightColoredActionLink(
          EduCoreBundle.message("hyperskill.review.topics.action.link"),
          SwitchTaskTabAction(project, 1)
        )
        linksPanel.add(showMoreInfo)
      }

      if (PostHyperskillProjectToGithub.isAvailable(task)) {
        val postToGithubLink = LightColoredActionLink(
          EduCoreBundle.message("hyperskill.action.post.to.github"),
          PostHyperskillProjectToGithub()
        )
        linksPanel.add(postToGithubLink)
      }
    }

    if (task.showAnswerHints) {
      val answerHintsPanel = createAnswerHintsPanel(project, task, checkResult)
      if (answerHintsPanel != null) {
        linksPanel.add(answerHintsPanel, BorderLayout.SOUTH)
      }
    }

    return linksPanel
  }

  private val Task.showAnswerHints: Boolean
    get() = status != CheckStatus.Unchecked || feedback?.time != null

  private fun createAnswerHintsPanel(project: Project, task: Task, checkResult: CheckResult): JPanel? {
    val answerHintsPanel = lazy(LazyThreadSafetyMode.NONE) {
      val panel = JPanel()
      panel.layout = BoxLayout(panel, BoxLayout.X_AXIS)
      panel
    }

    val peekSolution = when {
      !task.canShowSolution() -> null
      project.isStudentProject() -> {
        val isExternal = task.course is HyperskillCourse
        val text = EduCoreBundle.message("label.peek.solution") + if (isExternal) "" else "..."
        LightColoredActionLink(text, ActionManager.getInstance().getAction(CompareWithAnswerAction.ACTION_ID), isExternal = isExternal)
      }

      else -> null
    }
    peekSolution?.let { answerHintsPanel.value.add(it) }

    val diff = checkResult.diff
    if (diff != null) {
      //suppressing capitalization because LightColoredActionLink's base class requires Sentence capitalization for the parameter
      @Suppress("DialogTitleCapitalization")
      val compareOutputs = LightColoredActionLink(EduCoreBundle.message("label.compare.outputs"), CompareOutputsAction(project, diff))
      answerHintsPanel.value.add(compareOutputs)
    }

    return if (answerHintsPanel.isInitialized()) answerHintsPanel.value else null
  }

  private class ShowFullOutputAction(private val project: Project, private val text: String) : DumbAwareAction(null as String?) {
    private var outputShown = false

    //suppressing capitalization because LightColoredActionLink's base class requires Sentence capitalization for the parameter
    @Suppress("DialogTitleCapitalization")
    val actionLink: AnActionLink = LightColoredActionLink(EduCoreBundle.message("label.full.output.show"), this)

    override fun actionPerformed(e: AnActionEvent) {
      if (!outputShown) {
        CheckDetailsView.getInstance(project).showOutput(text)
        outputShown = true
        actionLink.text = EduCoreBundle.message("label.full.output.hide")
      }
      else {
        CheckDetailsView.getInstance(project).clear()
        outputShown = false
        actionLink.text = EduCoreBundle.message("label.full.output.show")
      }
    }
  }

  class SwitchTaskTabAction(private val project: Project, private val index: Int) : DumbAwareAction(null as String?) {
    override fun actionPerformed(e: AnActionEvent) {
      selectTab(project, index)
    }
  }

  companion object {
    fun selectTab(project: Project, index: Int): Content? {
      val window = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW)
      val tab = window?.contentManager?.getContent(index) ?: return null
      window.contentManager.setSelectedContent(tab)
      return tab
    }
  }

  private class CompareOutputsAction(private val project: Project, private val diff: CheckResultDiff) : DumbAwareAction(null as String?) {
    override fun actionPerformed(e: AnActionEvent) {
      val expected = DiffContentFactory.getInstance().create(diff.expected)
      val actual = DiffContentFactory.getInstance().create(diff.actual)
      val request = SimpleDiffRequest(
        diff.title, expected, actual, EduCoreBundle.message("compare.outputs.expected"),
        EduCoreBundle.message("compare.outputs.actual")
      )
      DiffManager.getInstance().showDiff(project, request, DiffDialogHints.FRAME)
    }
  }
}
