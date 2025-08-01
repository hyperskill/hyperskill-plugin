package org.hyperskill.academy.learning.checker

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse

class CheckActionListener : CheckListener {

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    println("Task ${task.name} has been checked")
    checkResultVerifier(task, result)
    checkFeedbackEqualsWithCheckResult(task, result)
    val messageProducer = expectedMessageProducer ?: return
    val expectedMessage = messageProducer(task) ?: error("Unexpected task `${task.name}`")
    assertEquals("Checking output for ${getTaskName(task)} fails", expectedMessage, result.message)
  }

  private fun checkFeedbackEqualsWithCheckResult(task: Task, checkResult: CheckResult) {
    val errorMessage = "Check result and saved feedback doesn't match for ${task.name}"
    val feedback = task.feedback ?: error("CheckFeedback should be filled out for ${task.lesson.name}/${task.name}")
    assertEquals(errorMessage, feedback.message, checkResult.message)
    assertEquals(errorMessage, feedback.expected, checkResult.diff?.expected)
    assertEquals(errorMessage, feedback.actual, checkResult.diff?.actual)
  }

  companion object {
    private val SHOULD_FAIL: (Task, CheckResult) -> Unit = { task, result ->
      val taskName = getTaskName(task)
      assertFalse("Check Task Action skipped for $taskName with type: ${task.itemType}", result.status == CheckStatus.Unchecked)
      assertFalse("Check Task Action passed for $taskName", result.status == CheckStatus.Solved)
      println("Checking status for $taskName: fails as expected")
    }

    private val SHOULD_PASS: (Task, CheckResult) -> Unit = { task, result ->
      val taskName = getTaskName(task)
      assertFalse("Check Task Action skipped for $taskName with type: ${task.itemType}", result.status == CheckStatus.Unchecked)
      assertFalse("Check Task Action failed for $taskName", result.status == CheckStatus.Failed)
      println("Checking status for $taskName: passes as expected")
    }

    private val SHOULD_SKIP: (Task, CheckResult) -> Unit = { task, result ->
      val taskName = getTaskName(task)
      assertFalse("Check Task Action passed for $taskName", result.status == CheckStatus.Solved)
      assertFalse("Check Task Action failed for $taskName", result.status == CheckStatus.Failed)
      println("Checking status for $taskName: skipped as expected")
    }

    private fun getTaskName(task: Task): String {
      val sectionPrefix = task.lesson.section?.let { "${it.name}/" } ?: ""
      return "$sectionPrefix${task.lesson.name}/${task.name}"
    }

    // Those fields can be modified if some special checks are needed (return true if should run standard checks)
    private var checkResultVerifier = SHOULD_PASS
    private var expectedMessageProducer: ((Task) -> String?)? = null

    fun registerListener(disposable: Disposable) {
      CheckListener.EP_NAME.point.registerExtension(CheckActionListener(), disposable)
    }

    fun reset() {
      setCheckResultVerifier(SHOULD_PASS)
      expectedMessageProducer = null
    }

    fun shouldFail() {
      setCheckResultVerifier(SHOULD_FAIL)
    }

    fun shouldSkip() {
      setCheckResultVerifier(SHOULD_SKIP)
    }

    fun expectedMessage(producer: (Task) -> String?) {
      expectedMessageProducer = producer
    }

    fun setCheckResultVerifier(verifier: (Task, CheckResult) -> Unit) {
      checkResultVerifier = verifier
    }
  }
}
