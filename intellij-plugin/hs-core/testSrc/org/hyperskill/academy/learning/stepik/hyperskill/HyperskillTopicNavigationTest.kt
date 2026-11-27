package org.hyperskill.academy.learning.stepik.hyperskill

import okhttp3.mockwebserver.MockResponse
import org.hyperskill.academy.learning.EduBrowser
import org.hyperskill.academy.learning.MockEduBrowser
import org.hyperskill.academy.learning.MockResponseFactory
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.pathWithoutPrams
import org.hyperskill.academy.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase
import org.junit.Test
import java.net.HttpURLConnection.HTTP_NO_CONTENT

class HyperskillTopicNavigationTest : HyperskillProjectOpenerTestBase() {
  private val completedSteps = mutableListOf<Int>()
  private var nextActivity: String? = null

  @Test
  fun `test completed topic continues with the next topic in the problems project`() {
    val course = createProblemsProject()
    nextActivity = """{"type": 10, "target_id": $NEXT_TOPIC_THEORY, "topic_id": $NEXT_TOPIC}"""
    configureResponses()

    openNextActivity(project, findTask(0, 0, 1))

    assertSame(course, project.course)
    assertNull(mockProjectOpener.courseOpenedInNewProject)
    val nextLesson = course.getTopicsSection()!!.lessons[1]
    assertEquals(listOf(NEXT_TOPIC_THEORY, NEXT_TOPIC_QUIZ), nextLesson.taskList.map { it.id })
    assertInstanceOf(nextLesson.taskList[1], QuizTask::class.java)
    assertNull((EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl)
  }

  @Test
  fun `test completed topic continues with the stage in the IDE project of the hyperskill project`() {
    val course = createProblemsProject()
    configureMockResponsesForStages()
    nextActivity = """{"type": 20, "target_id": $NEXT_STAGE}"""
    configureResponses(selectedProject = 1)

    openNextActivity(project, findTask(0, 0, 1))

    assertNull("A stage must not be added to the problems project", course.getProjectLesson())
    val openedCourse = mockProjectOpener.courseOpenedInNewProject as HyperskillCourse
    assertEquals(1, openedCourse.hyperskillProject?.id)
    assertEquals(NEXT_STAGE, openedCourse.selectedStage)
  }

  @Test
  fun `test completed topic opens its page if the study plan still points to it`() {
    createProblemsProject()
    nextActivity = """{"type": 10, "target_id": $CURRENT_TOPIC_THEORY, "topic_id": $CURRENT_TOPIC}"""
    configureResponses()

    openNextActivity(project, findTask(0, 0, 1))

    assertTopicPageIsOpened()
  }

  @Test
  fun `test completed topic opens its page if the next activity is not available in the IDE`() {
    createProblemsProject()
    nextActivity = """{"type": 0, "target_id": null}"""
    configureResponses()

    openNextActivity(project, findTask(0, 0, 1))

    assertTopicPageIsOpened()
  }

  @Test
  fun `test completed topic opens its page at the end of the study plan`() {
    createProblemsProject()
    configureResponses()

    openNextActivity(project, findTask(0, 0, 1))

    assertTopicPageIsOpened()
  }

  @Test
  fun `test theory is completed on the server when the learner moves on`() {
    createProblemsProject()
    configureResponses()
    val theory = findTask(0, 0, 0)

    completeTheoryStepInBackground(project, theory)

    assertEquals(listOf(CURRENT_TOPIC_THEORY), completedSteps)
    assertEquals(CheckStatus.Solved, theory.status)
  }

  @Test
  fun `test solved theory is not completed again`() {
    createProblemsProject()
    configureResponses()
    val theory = findTask(0, 0, 0)
    theory.status = CheckStatus.Solved

    completeTheoryStepInBackground(project, theory)

    assertEmpty(completedSteps)
  }

  @Test
  fun `test other tasks are not completed when the learner moves on`() {
    createProblemsProject()
    configureResponses()
    val quiz = findTask(0, 0, 1)

    completeTheoryStepInBackground(project, quiz)

    assertEmpty(completedSteps)
    assertEquals(CheckStatus.Unchecked, quiz.status)
  }

  private fun assertTopicPageIsOpened() {
    assertEquals(topicCompletedLink(CURRENT_TOPIC), (EduBrowser.getInstance() as MockEduBrowser).lastVisitedUrl)
    assertEquals(1, (project.course as HyperskillCourse).getTopicsSection()!!.lessons.size)
  }

  private fun createProblemsProject(): HyperskillCourse =
    hyperskillCourseWithFiles(projectId = null, name = getProblemsProjectName(FakeGradleBasedLanguage.id)) {
      section(HYPERSKILL_TOPICS) {
        lesson("Current topic") {
          theoryTask("Current theory", stepId = CURRENT_TOPIC_THEORY) {
            taskFile("Task.txt", "")
          }
          quizTask("Current quiz", stepId = CURRENT_TOPIC_QUIZ) {
            taskFile("Task.txt", "")
          }
        }
      }
    }

  private fun configureResponses(selectedProject: Int? = null) {
    val steps = mapOf(
      CURRENT_TOPIC_THEORY to step(CURRENT_TOPIC_THEORY, CURRENT_TOPIC, "text", "Current topic"),
      CURRENT_TOPIC_QUIZ to step(CURRENT_TOPIC_QUIZ, CURRENT_TOPIC, "choice", "Current quiz"),
      NEXT_TOPIC_THEORY to step(NEXT_TOPIC_THEORY, NEXT_TOPIC, "text", "Next topic"),
      NEXT_TOPIC_QUIZ to step(NEXT_TOPIC_QUIZ, NEXT_TOPIC, "choice", "Next quiz"),
    )

    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      val url = request.requestUrl ?: return@withResponseHandler null
      when (val path = request.pathWithoutPrams) {
        "/api/learning-activities/next" -> response("""{"learning-activities": [${nextActivity ?: ""}]}""")

        "/api/profiles/current" -> response("""{"profiles": [{"id": 1, "project": $selectedProject, "is_guest": false}]}""")

        "/api/steps" -> {
          val requestedStep = url.queryParameter("ids")?.toIntOrNull()
          val requestedTopic = url.queryParameter("topic")?.toIntOrNull()
          when {
            requestedStep != null -> steps[requestedStep]?.let { stepsResponse(listOf(it)) }
            requestedTopic == CURRENT_TOPIC -> stepsResponse(listOf(steps.getValue(CURRENT_TOPIC_THEORY), steps.getValue(CURRENT_TOPIC_QUIZ)))
            requestedTopic == NEXT_TOPIC -> stepsResponse(listOf(steps.getValue(NEXT_TOPIC_THEORY), steps.getValue(NEXT_TOPIC_QUIZ)))
            else -> null
          }
        }

        else -> {
          val completedStep = COMPLETE_STEP_PATH.matchEntire(path)?.groupValues?.get(1)?.toInt() ?: return@withResponseHandler null
          completedSteps += completedStep
          MockResponseFactory.fromString("", HTTP_NO_CONTENT)
        }
      }
    }
  }

  private fun stepsResponse(steps: List<String>): MockResponse =
    response("""{"meta": {"page": 1, "has_next": false, "has_previous": false}, "steps": [${steps.joinToString(",")}]}""")

  private fun response(text: String): MockResponse = MockResponseFactory.fromString(text)

  private fun step(id: Int, topic: Int, blockName: String, title: String): String = """
    {
      "id": $id,
      "topic": $topic,
      "title": "$title",
      "position": 1,
      "is_completed": true,
      "is_next": false,
      "is_recommended": true,
      "block": {"name": "$blockName", "text": "<p>Text</p>", "options": {}}
    }
  """

  companion object {
    private val COMPLETE_STEP_PATH = """/api/steps/(\d+)/complete""".toRegex()

    private const val CURRENT_TOPIC = 1
    private const val CURRENT_TOPIC_THEORY = 11
    private const val CURRENT_TOPIC_QUIZ = 12

    private const val NEXT_TOPIC = 2
    private const val NEXT_TOPIC_THEORY = 21
    private const val NEXT_TOPIC_QUIZ = 22

    private const val NEXT_STAGE = 1
  }
}
