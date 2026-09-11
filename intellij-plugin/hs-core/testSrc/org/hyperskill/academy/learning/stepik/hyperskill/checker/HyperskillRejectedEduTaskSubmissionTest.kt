package org.hyperskill.academy.learning.stepik.hyperskill.checker

import okhttp3.mockwebserver.MockResponse
import org.hyperskill.academy.learning.MockResponseFactory
import org.hyperskill.academy.learning.actions.CheckAction
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.pathWithoutPrams
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import org.hyperskill.academy.learning.testAction
import org.hyperskill.academy.learning.ui.getUICheckLabel
import org.hyperskill.academy.learning.withNotificationCheck
import org.intellij.lang.annotations.Language
import org.junit.Test
import java.net.HttpURLConnection.HTTP_FORBIDDEN

/**
 * A stage locked behind a subscription is checked locally as any other stage, but the solution for it is not accepted
 * by JBA. The rejection has to reach the learner instead of being hidden behind the local test results.
 */
class HyperskillRejectedEduTaskSubmissionTest : HyperskillActionTestBase() {

  override fun createCourse() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask(stepId = 1) {
          checkResultFile(CheckStatus.Solved)
        }
      }
    } as HyperskillCourse
    course.stages = listOf(HyperskillStage(1, "", 1))
    course.hyperskillProject = HyperskillProject()
  }

  @Test
  fun `test rejection reported with an error code is shown and stage is not completed`() {
    configureResponses(MockResponseFactory.fromString(forbiddenSubmission, HTTP_FORBIDDEN))
    doCheckAndAssertRejected()
  }

  @Test
  fun `test rejection reported within a created submission is shown and stage is not completed`() {
    configureResponses(MockResponseFactory.fromString(rejectedSubmission))
    doCheckAndAssertRejected()
  }

  @Test
  fun `test accepted submission keeps the stage solved`() {
    configureResponses(MockResponseFactory.fromString(acceptedSubmission))
    val task = projectTask

    withNotificationCheck(project, testRootDisposable, { shown, _ ->
      assertFalse("No notification is expected for an accepted solution", shown)
    }) {
      checkTask(task)
    }

    assertEquals(CheckStatus.Solved, task.status)
    assertTrue("Stage is expected to be completed", hyperskillCourse.stages.single().isCompleted)
    assertEquals(1, SubmissionsManager.getInstance(project).getSubmissionsFromMemory(setOf(task.id)).size)
  }

  private fun doCheckAndAssertRejected() {
    val task = projectTask

    withNotificationCheck(project, testRootDisposable, { shown, content ->
      assertTrue("Notification about the rejected solution is expected", shown)
      assertTrue("Notification is expected to contain the reason, but was: `$content`", content.contains(REJECTION_REASON))
    }) {
      checkTask(task)
    }

    assertEquals("Solution was not accepted, so the task must not stay solved", CheckStatus.Unchecked, task.status)
    assertEquals(REJECTION_REASON, task.feedback?.message)
    assertFalse("Stage is not expected to be completed", hyperskillCourse.stages.single().isCompleted)
    assertTrue(
      "Not accepted submission must not be stored",
      SubmissionsManager.getInstance(project).getSubmissionsFromMemory(setOf(task.id)).isEmpty()
    )
  }

  private fun checkTask(task: Task) {
    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
  }

  private val hyperskillCourse: HyperskillCourse get() = getCourse() as HyperskillCourse

  private val projectTask: Task get() = hyperskillCourse.getProjectLesson()!!.taskList.single()

  private fun configureResponses(submissionResponse: MockResponse) {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      // the requests the check makes besides these two are left to the default `not found` response
      when (request.pathWithoutPrams) {
        "/api/attempts" -> MockResponseFactory.fromString(attempt)
        "/api/submissions" -> submissionResponse
        else -> null
      }
    }
  }

  @Language("JSON")
  private val attempt = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "attempts": [
        {
          "dataset": "",
          "id": 7565800,
          "status": "active",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """

  @Language("JSON")
  private val forbiddenSubmission = """
    {
      "detail": "$REJECTION_REASON"
    }
  """

  @Language("JSON")
  private val rejectedSubmission = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "7565800",
          "id": "7565003",
          "status": "wrong",
          "hint": "$REJECTION_REASON",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """

  @Language("JSON")
  private val acceptedSubmission = """
    {
      "meta": {
        "page": 1,
        "has_next": false,
        "has_previous": false
      },
      "submissions": [
        {
          "attempt": "7565800",
          "id": "7565003",
          "status": "correct",
          "hint": "Congratulations!",
          "step": 1,
          "time": "2020-04-29T11:44:20.422Z",
          "user": 6242591
        }
      ]
    }
  """

  companion object {
    private const val REJECTION_REASON = "Can't post a submission for this stage. Upgrade your subscription."
  }
}
