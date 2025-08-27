package org.hyperskill.academy.learning.submissions.ui

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.hyperskill.academy.learning.EduExperimentalFeatures
import org.hyperskill.academy.learning.JavaUILibrary
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.invokeLater
import org.hyperskill.academy.learning.isFeatureEnabled
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.submissions.*
import org.hyperskill.academy.learning.submissions.ui.linkHandler.LoginLinkHandler
import org.hyperskill.academy.learning.submissions.ui.linkHandler.SubmissionsDifferenceLinkHandler
import org.hyperskill.academy.learning.submissions.ui.linkHandler.SubmissionsDifferenceLinkHandler.Companion.getSubmissionDiffLink
import org.hyperskill.academy.learning.taskToolWindow.links.SwingToolWindowLinkHandler
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.StyleManager
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.SwingTextPanel
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TaskToolWindowCardTextTab
import java.util.concurrent.CompletableFuture

/**
 * Constructor is called exclusively in [org.hyperskill.academy.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
open class SubmissionsTab(project: Project) : TaskToolWindowCardTextTab(project) {

  override val uiMode: JavaUILibrary
    get() = JavaUILibrary.SWING

  protected val panel: SwingTextPanel
    get() = cards().first() as SwingTextPanel

  override fun update(task: Task) {
    if (!task.supportSubmissions) return

    CompletableFuture.runAsync({
      val submissionsManager = SubmissionsManager.getInstance(project)
      val isLoggedIn = submissionsManager.isLoggedIn()

      updateContent(task, isLoggedIn)
    }, ProcessIOExecutorService.INSTANCE)
  }

  protected open fun updateContent(task: Task, isLoggedIn: Boolean) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    val (descriptionText, customLinkHandler) = prepareSubmissionsContent(submissionsManager, task, isLoggedIn)
    project.invokeLater {
      updatePanel(panel, descriptionText, customLinkHandler)
    }
  }

  protected fun updatePanel(panel: SwingTextPanel, text: String, linkHandler: SwingToolWindowLinkHandler?) = panel.apply {
    hideLoadingSubmissionsPanel()
    updateLinkHandler(linkHandler)
    setText(text)
  }

  @RequiresBackgroundThread
  protected fun prepareSubmissionsContent(
    submissionsManager: SubmissionsManager,
    task: Task,
    isLoggedIn: Boolean
  ): Pair<String, SwingToolWindowLinkHandler?> {
    val submissionsList = submissionsManager.getSubmissionsFromMemory(setOf(task.id))

    if (!isLoggedIn) {
      return LoginLinkHandler.getLoginText() to LoginLinkHandler(project, submissionsManager)
    }

    if (submissionsList.isEmpty()) {
      return emptySubmissionsMessage() to null
    }

    return getSubmissionsText(submissionsList, isToShowSubmissionsIds(task)).toString() to
      SubmissionsDifferenceLinkHandler(project, task, submissionsManager)
  }

  fun showLoadingPanel(platformName: String) = panel.showLoadingSubmissionsPanel(platformName)

  protected fun getSubmissionsText(
    submissionsNext: List<Submission>,
    isToShowSubmissionsIds: Boolean = false,
  ): StringBuilder = submissionsNext.map {
    submissionLink(it, isToShowSubmissionsIds)
  }.joinTo(
    StringBuilder(OPEN_UL_TAG), separator = ""
  ).append(CLOSE_UL_TAG)

  companion object {
    const val SUBMISSION_PROTOCOL = "submission://"

    private const val OPEN_UL_TAG = "<ul style=list-style-type:none;margin:0;padding:0;>"
    private const val CLOSE_UL_TAG = "</ul>"

    val textStyleHeader: String
      get() = StyleManager().textStyleHeader

    private fun emptySubmissionsMessage(): String = "<a $textStyleHeader>${EduCoreBundle.message("submissions.empty")}"

    /**
     * Showing submissions ids is needed for `ApplyHyperskillSubmission` action testing
     */
    private fun isToShowSubmissionsIds(task: Task) = false

    private fun submissionLink(submission: Submission, isToShowSubmissionsIds: Boolean): String? {
      val time = submission.time ?: return null
      val pictureSize = StyleManager().bodyLineHeight
      val date = formatDate(time)
      val text = if (isToShowSubmissionsIds) {
        "$date submission.id = ${submission.id}"
      }
      else {
        date
      }

      return "<li><h><img src=${getImageUrl(submission.status)} hspace=6 width=${pictureSize} height=${pictureSize}/></h>" +
             "<a $textStyleHeader;color:${getLinkColor(submission)} href=${getSubmissionDiffLink(submission.id)}> ${text}</a></li>"
    }
  }
}
