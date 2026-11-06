package org.hyperskill.academy.learning.stepik.hyperskill.checker

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.mockwebserver.MockResponse
import org.hyperskill.academy.learning.MockResponseFactory
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.pathWithoutPrams
import org.junit.Test

class HyperskillCheckQuizTaskTest : HyperskillCheckActionTestBase() {
  private val postedSubmissions = mutableListOf<String>()

  override fun createCourse() {
    courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson("Problems") {
        quizTask("quiz", stepId = STEP_ID) {
          taskFile("Task.txt", "")
        }
      }
    }
  }

  @Test
  fun `test correct answer is sent to the server and solves the task`() {
    configureResponses(submissions(status = "correct"))
    val task = answeredTask()

    checkCheckAction(task, CheckStatus.Solved, "<html>Correct solution</html>")

    assertEquals(CheckStatus.Solved, task.status)
    val submission = ObjectMapper().readTree(postedSubmissions.single())
    assertEquals(ATTEMPT_ID, submission["attempt"].asInt())
    // The server validates the reply against the block type, so nothing but the answer has to be there
    assertEquals("""{"choices":[true,false]}""", submission["reply"].toString())
  }

  @Test
  fun `test wrong answer fails the task with the hint from the server`() {
    configureResponses(submissions(status = "wrong", hint = "Think again"))
    val task = answeredTask()

    checkCheckAction(task, CheckStatus.Failed, "Think again")

    assertEquals(CheckStatus.Failed, task.status)
  }

  @Test
  fun `test rejected answer is not a wrong one`() {
    configureResponses(submissions(status = "rejected", hint = "The limit of submissions is reached"))
    val task = answeredTask()

    checkCheckAction(task, CheckStatus.Unchecked, "The limit of submissions is reached")

    assertEquals(CheckStatus.Unchecked, task.status)
  }

  @Test
  fun `test nothing is sent without an answer`() {
    configureResponses(submissions(status = "correct"))
    val task = findTask(0, 0) as QuizTask
    task.attempt = attempt()

    checkCheckAction(task, CheckStatus.Unchecked, EduCoreBundle.message("hyperskill.quiz.no.answer"))

    assertEmpty(postedSubmissions)
  }

  @Test
  fun `test without the form the check finds a solution from the website`() {
    configureResponses(submissions(status = "correct"))
    val task = findTask(0, 0) as QuizTask

    checkCheckAction(task, CheckStatus.Solved)

    assertEmpty(postedSubmissions)
    assertEquals(CheckStatus.Solved, task.status)
  }

  @Test
  fun `test retry starts a new attempt and forgets the answer`() {
    configureResponses(submissions(status = "wrong"))
    val task = answeredTask()

    val result = HyperskillRemoteTaskChecker().retry(task)

    assertEquals(Ok(true), result)
    assertEquals(NEW_ATTEMPT_ID, task.attempt?.id)
    assertNull(task.reply)
  }

  private fun answeredTask(): QuizTask {
    val task = findTask(0, 0) as QuizTask
    task.attempt = attempt()
    task.reply = mapOf("choices" to listOf(true, false))
    return task
  }

  private fun attempt(): Attempt = Attempt(STEP_ID).apply { id = ATTEMPT_ID }

  private fun configureResponses(submissionsResponse: String) {
    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      when (request.pathWithoutPrams) {
        "/api/submissions" -> {
          if (request.method == "POST") {
            postedSubmissions += request.body.readUtf8()
          }
          response(submissionsResponse)
        }

        "/api/attempts" -> response(
          """{"meta": {"page": 1, "has_next": false, "has_previous": false}, "attempts": [{"id": $NEW_ATTEMPT_ID, "status": "active", "step": $STEP_ID}]}"""
        )

        else -> response("{}")
      }
    }
  }

  private fun response(text: String): MockResponse = MockResponseFactory.fromString(text)

  private fun submissions(status: String, hint: String = ""): String = """
    {
      "meta": {"page": 1, "has_next": false, "has_previous": false},
      "submissions": [
        {"id": 31, "attempt": $ATTEMPT_ID, "status": "$status", "hint": "$hint", "step": $STEP_ID, "time": "2026-01-01T00:00:00Z"}
      ]
    }
  """

  companion object {
    private const val STEP_ID = 4
    private const val ATTEMPT_ID = 21
    private const val NEW_ATTEMPT_ID = 22
  }
}
