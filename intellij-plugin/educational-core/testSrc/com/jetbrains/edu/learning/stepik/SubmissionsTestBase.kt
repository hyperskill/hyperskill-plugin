package org.hyperskill.academy.learning.stepik

import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.actions.CheckAction
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import org.hyperskill.academy.learning.testAction
import org.hyperskill.academy.learning.ui.getUICheckLabel
import java.util.concurrent.CompletableFuture

abstract class SubmissionsTestBase : EduTestCase() {

  protected fun doTestSubmissionsLoaded(taskIds: Set<Int>, taskIdsToSubmissionsNumber: Map<Int, Int>) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    assertEmpty(
      "SubmissionsManager should not contain submissions before submission loading",
      submissionsManager.getSubmissionsFromMemory(taskIds)
    )
    CompletableFuture.runAsync { submissionsManager.prepareSubmissionsContentWhenLoggedIn() }
      .thenApply {
        for (taskId in taskIds) {
          val submissionsNumber = taskIdsToSubmissionsNumber[taskId] ?: error("Number of submissions for taskId $taskId is null")
          checkSubmissionsPresent(submissionsManager, taskId, submissionsNumber)
        }
      }
  }

  private fun checkSubmissionPresentWithStatus(
    submissionsManager: SubmissionsManager,
    taskId: Int,
    checkStatus: String
  ) {
    val submissions = submissionsManager.getSubmissionsFromMemory(setOf(taskId))
    assertNotEmpty(submissions)
    assertEquals(1, submissions.size)
    assertEquals(checkStatus, submissions[0].status)
  }

  protected fun checkSubmissionsPresent(
    submissionsManager: SubmissionsManager,
    taskId: Int,
    submissionsNumber: Int = 1
  ) {
    val submissions = submissionsManager.getSubmissionsFromMemory(setOf(taskId))
    assertNotEmpty(submissions)
    assertTrue(submissions.size == submissionsNumber)
  }

  protected fun doTestSubmissionAddedAfterTaskCheck(taskId: Int, checkStatus: String) {
    val submissionsManager = SubmissionsManager.getInstance(project)
    assertEmpty(
      "SubmissionsManager should not contain submissions before task check",
      submissionsManager.getSubmissionsFromMemory(setOf(taskId))
    )

    CompletableFuture.runAsync { checkTask() }.thenApply { checkSubmissionPresentWithStatus(submissionsManager, taskId, checkStatus) }
  }

  private fun checkTask(lessonIndex: Int = 0, taskIndex: Int = 0) {
    val task = findTask(lessonIndex, taskIndex)
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
  }
}
