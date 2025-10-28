package org.hyperskill.academy.ai.debugger.core.listener

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.hyperskill.academy.ai.debugger.core.log.AIDebuggerLogEntry
import org.hyperskill.academy.ai.debugger.core.log.logInfo
import org.hyperskill.academy.ai.debugger.core.log.toTaskData
import org.hyperskill.academy.ai.debugger.core.messages.AIDebuggerCoreBundle
import org.hyperskill.academy.ai.debugger.core.session.AIDebugSessionService
import org.hyperskill.academy.ai.debugger.core.ui.AIDebuggerHintInlineBanner
import org.hyperskill.academy.ai.debugger.core.utils.AIDebugUtils.collectTestInfo
import org.hyperskill.academy.ai.debugger.core.utils.AIDebugUtils.toNameTextMap
import org.hyperskill.academy.learning.checker.CheckListener
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.EduTestInfo.Companion.firstFailed
import org.hyperskill.academy.learning.courseFormat.ext.getVirtualFile
import org.hyperskill.academy.learning.courseFormat.ext.project
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView

class AIDebuggerCheckListener : CheckListener {
  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    AIDebuggerLogEntry(
      task = task.toTaskData(),
      actionType = "TestInfo",
      firstFailedTestName = result.executedTestsInfo.firstFailed()?.name ?: "",
      testResult = result,
    ).logInfo()

    if (!isAvailable(task, result)) return
    val textToShow = AIDebuggerCoreBundle.message("action.HyperskillEducational.AiDebuggerNotification.text")

    val aiDebuggerHintBanner = AIDebuggerHintInlineBanner(project, task, textToShow).apply {
      addAction(AIDebuggerCoreBundle.message("action.HyperskillEducational.AiDebuggerNotification.start.debugging.session")) {
        showDebugNotification(task, result) { this.close() }
      }
    }
    TaskToolWindowView.getInstance(project).addInlineBannerToCheckPanel(aiDebuggerHintBanner)
    AIDebuggerLogEntry(
      task = task.toTaskData(),
      actionType = "AIDebuggingNotificationBanner",
      testResult = result,
    ).logInfo()
  }

  private fun showDebugNotification(task: Task, testResult: CheckResult, closeAIDebuggingHint: () -> Unit) {
    val project = task.project ?: error("Project is missing")
    val taskFiles = task.taskFiles.values.filter { it.isVisible }.takeIf { it.isNotEmpty() } ?: return
    val userSolution = taskFiles.toNameTextMap(project).filter { it.value.isNotBlank() }
    val virtualFileMap = runReadAction {
      taskFiles.associate { it.name to (it.getVirtualFile(project) ?: error("Virtual file is not found")) }
    }
    val testInfo = testResult.collectTestInfo(project, task)
    project.service<AIDebugSessionService>()
      .runDebuggingSession(task, userSolution, virtualFileMap, testResult, testInfo, closeAIDebuggingHint)
    AIDebuggerLogEntry(
      task = task.toTaskData(),
      actionType = "StartDebugSessionIsClicked",
      testResult = testResult,
      testInfo = testInfo,
      userCode = userSolution.toString(),
    ).logInfo()
  }

  // TODO: when should we show this button?
  private fun isAvailable(task: Task, result: CheckResult) =
    task.course.courseMode == CourseMode.STUDENT &&
    task.status == CheckStatus.Failed &&
    task is EduTask &&
    result.executedTestsInfo.firstFailed() != null
}
