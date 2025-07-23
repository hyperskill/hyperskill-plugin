package org.hyperskill.academy.learning.stepik.hyperskill.checker

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.text.nullize
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.CodeTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.UnsupportedTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.messages.EduFormatBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.stepik.api.StepikBasedConnector.Companion.getStepikBasedConnector
import org.hyperskill.academy.learning.stepik.api.StepikBasedSubmission
import org.hyperskill.academy.learning.stepik.hyperskill.HYPERSKILL_DEFAULT_HOST
import org.hyperskill.academy.learning.stepik.hyperskill.HYPERSKILL_URL
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillLoginListener
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.markStageAsCompleted
import org.hyperskill.academy.learning.stepik.hyperskill.submissions.HyperskillSubmissionFactory
import org.hyperskill.academy.learning.submissions.SolutionFile
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import org.hyperskill.academy.learning.submissions.getSolutionFiles
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.TimeUnit

object HyperskillCheckConnector {
  const val EVALUATION_STATUS = "evaluation"

  private val LOG = Logger.getInstance(HyperskillCheckConnector::class.java)
  private val CODE_TASK_CHECK_TIMEOUT = TimeUnit.MINUTES.toSeconds(2)

  private val loginListener: HyperskillLoginListener
    get() = HyperskillLoginListener

  fun isRemotelyChecked(task: Task): Boolean = when (task) {
    is CodeTask,
    is RemoteEduTask,
    is UnsupportedTask -> true

    else -> false
  }

  private fun periodicallyCheckSubmissionResult(project: Project, submission: StepikBasedSubmission, task: Task): CheckResult {
    require(isRemotelyChecked(task)) { "Task is not checked remotely" }

    val submissionId = submission.id ?: error("Submission must have id")
    val connector = task.getStepikBasedConnector()

    var lastSubmission = submission
    var delay = 1L
    val timeout = if (isUnitTestMode) 5L else CODE_TASK_CHECK_TIMEOUT
    while (delay < timeout && lastSubmission.status == EVALUATION_STATUS) {
      TimeUnit.SECONDS.sleep(delay)
      delay *= 2
      lastSubmission = connector.getSubmission(submissionId).onError { return it.toCheckResult() }
    }

    if (lastSubmission.status != EVALUATION_STATUS) {
      if (task.supportSubmissions) {
        SubmissionsManager.getInstance(project).addToSubmissions(task.id, lastSubmission)
      }
      return lastSubmission.toCheckResult()
    }

    return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.failed.to.get.check.result.from", connector.platformName))
  }

  fun postEduTaskSolution(task: Task, project: Project, result: CheckResult) {
    when (val attemptResponse = HyperskillConnector.getInstance().postAttempt(task)) {
      is Err -> showErrorDetails(project, attemptResponse.error)
      is Ok -> {
        val feedback = if (result.details == null) result.message.xmlUnescaped else "${result.message.xmlUnescaped}\n${result.details}"
        postEduSubmission(attemptResponse.value, project, task, feedback)
        checkStageToBeCompleted(task)
      }
    }
  }

  private fun postEduSubmission(attempt: Attempt, project: Project, task: Task, feedback: String) {
    val files = getSolutionFilesResult(project, task).onError { error ->
      showErrorDetails(project, EduCoreBundle.message("error.failed.to.collect.files", task.name))
      LOG.error(error)
      return
    }
    val submission = HyperskillSubmissionFactory.createEduTaskSubmission(task, attempt, files, feedback)
    when (val response = HyperskillConnector.getInstance().postSubmission(submission)) {
      is Err -> showErrorDetails(project, response.error)
      is Ok -> SubmissionsManager.getInstance(project).addToSubmissionsWithStatus(task.id, task.status, response.value)
    }
  }

  private fun getSolutionFilesResult(project: Project, task: Task): Result<List<SolutionFile>, String> {
    val files = try {
      getSolutionFiles(project, task)
    }
    catch (e: IllegalStateException) {
      return Err("Unable to create submission for the task ${task.name}: ${e.message}")
    }
    return Ok(files)
  }

  private fun checkCodeTaskWithWebSockets(project: Project, task: CodeTask): Result<CheckResult, SubmissionError> {
    val connector = HyperskillConnector.getInstance()
    val webSocketConfiguration = connector.getWebSocketConfiguration().onError { error ->
      return Err(SubmissionError.NoSubmission(error))
    }

    val initialState = InitialState(project, task, webSocketConfiguration.token)
    // TODO: remove `cf_protocol_version=v2` after full transfer to the cf protocol version 2 (~Summer 2023).
    val finalState = connector.connectToWebSocketWithTimeout(
      CODE_TASK_CHECK_TIMEOUT,
      "wss://${getWebsocketHostName()}/ws/connection/websocket?cf_protocol_version=v2",
      initialState
    )

    return finalState.getResult()
  }

  private fun getWebsocketHostName(): String {
    return try {
      URL(HYPERSKILL_URL).host
    }
    catch (_: MalformedURLException) {
      return HYPERSKILL_DEFAULT_HOST
    }
  }

  fun checkCodeTask(project: Project, task: CodeTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    return checkCodeTaskWithWebSockets(project, task).onError { submissionError ->
      LOG.info(submissionError.error)
      val submission = when (submissionError) {
        is SubmissionError.NoSubmission -> HyperskillSubmitConnector.submitCodeTask(project, task).onError { error ->
          return failedToSubmit(project, task, error)
        }

        is SubmissionError.WithSubmission -> submissionError.submission
      }

      return periodicallyCheckSubmissionResult(project, submission, task)
    }
  }

  fun checkRemoteEduTask(project: Project, task: RemoteEduTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    val files = getSolutionFilesResult(project, task).onError {
      LOG.error(it)
      return CheckResult.failedToCheck
    }

    val submission = HyperskillSubmitConnector.submitRemoteEduTask(task, files).onError { error ->
      return failedToSubmit(project, task, error)
    }

    val result = periodicallyCheckSubmissionResult(project, submission, task)
    checkStageToBeCompleted(task)
    return result
  }

  private fun checkStageToBeCompleted(task: Task) {
    val course = task.course as HyperskillCourse
    if (course.isTaskInProject(task) && task.status == CheckStatus.Solved) {
      markStageAsCompleted(task)
    }
  }

  fun checkUnsupportedTask(task: UnsupportedTask): CheckResult {
    val checkIdResult = task.checkId()
    if (checkIdResult != null) {
      return checkIdResult
    }

    val connector = task.getStepikBasedConnector()
    val submissions = connector.getSubmissions(task.id)

    if (submissions.isEmpty()) {
      return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("hyperskill.unsupported.check.task.no.submissions"))
    }

    if (submissions.any { it.toCheckResult().isSolved }) {
      return CheckResult.SOLVED
    }

    return submissions.first().toCheckResult()
  }

  private fun showErrorDetails(project: Project, error: String) {
    if (error == EduFormatBundle.message("error.access.denied") || error == EduCoreBundle.message("error.failed.to.refresh.tokens")) {
      EduNotificationManager
        .create(ERROR, EduCoreBundle.message("error.failed.to.post.solution"), EduCoreBundle.message("error.access.denied.with.link"))
        .apply {
          addAction(NotificationAction.createSimpleExpiring(EduCoreBundle.message("notification.hyperskill.no.next.activity.login.action")) {
            this@apply.expire()
            loginListener.doLogin()
          })
        }
        .notify(project)
      return
    }

    LOG.warn(error)
    EduNotificationManager.create(
      ERROR,
      EduCoreBundle.message("error.failed.to.post.solution"),
      EduFormatBundle.message("help.use.guide", EduNames.FAILED_TO_POST_TO_JBA_URL)
    ).addAction(NotificationAction.createSimpleExpiring("Open in Browser") {
      EduBrowser.getInstance().browse(EduNames.FAILED_TO_POST_TO_JBA_URL)
    })
      .notify(project)
  }

  fun failedToSubmit(project: Project, task: Task, error: String): CheckResult {
    LOG.error(error)

    val platformName = task.getStepikBasedConnector().platformName
    val message = EduCoreBundle.message("stepik.base.failed.to.submit.task", task.itemType, platformName)

    showErrorDetails(project, error)

    return CheckResult(CheckStatus.Unchecked, message)
  }

  fun StepikBasedSubmission.toCheckResult(): CheckResult {
    val status = status ?: return CheckResult.failedToCheck
    val isSolved = status != "wrong"
    var message = hint.nullize() ?: "${StringUtil.capitalize(status)} solution"
    if (isSolved) {
      message = "<html>$message</html>"
    }
    return CheckResult(if (isSolved) CheckStatus.Solved else CheckStatus.Failed, message)
  }

  private fun String.toCheckResult(): CheckResult {
    return if (this == EduFormatBundle.message("error.access.denied")) {
      CheckResult(
        CheckStatus.Unchecked,
        EduCoreBundle.message("error.access.denied.with.link"),
        hyperlinkAction = { loginListener.doLogin() }
      )
    }
    else CheckResult(CheckStatus.Unchecked, this)
  }

  private fun Task.checkId(): CheckResult? {
    if (id == 0) {
      val link = feedbackLink ?: return CheckResult.failedToCheck
      return CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("check.result.corrupted.task", link))
    }
    return null
  }
}
