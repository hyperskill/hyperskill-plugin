package org.hyperskill.academy.learning.stepik.hyperskill.projectOpen

import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.progress.EmptyProgressIndicator
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.ext.CourseValidationResult
import org.hyperskill.academy.learning.courseFormat.ext.PluginsRequired
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.hasParams
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.pathWithoutPrams
import org.hyperskill.academy.learning.stepik.hyperskill.getProblemsProjectName
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepRequest
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillCourse
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillCourseWithFiles
import org.hyperskill.academy.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.StepInfo
import org.hyperskill.academy.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.TopicInfo
import org.junit.Test


class HyperskillProjectOpenTopicProblemsTest : HyperskillProjectOpenerTestBase() {

  private val stepSourceRequestCounts = mutableMapOf<Int, Int>()

  override fun setUp() {
    super.setUp()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()
  }

  @Test
  fun `test open non dataset problem with language chosen by user`() {
    val request = HyperskillOpenStepRequest(step10960.id, "TEXT", true)
    assertThrows(IllegalStateException::class.java) {
      mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, request)
    }
  }

  @Test
  fun `test open non dataset problem with supported language chosen by user`() {
    val request = HyperskillOpenStepRequest(step10960.id, FakeGradleBasedLanguage.id, true)
    assertThrows(IllegalStateException::class.java) {
      mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, request)
    }
  }

  @Test
  fun `test get course loads step source once`() {
    val request = HyperskillOpenStepRequest(step2640.id, FakeGradleBasedLanguage.id)

    val course = HyperskillOpenInIdeRequestHandler.getCourse(request, EmptyProgressIndicator())
      .onError { error("Course should be created: $it") } as HyperskillCourse

    assertProblemLoaded(course, TOPIC_85_NAME, step2640.title)
    assertStepSourceRequestedOnce(step2640)
  }

  @Test
  fun `test open problem reuses problems project`() {
    val problemsCourse = hyperskillCourseWithFiles(
      projectId = null,
      name = getProblemsProjectName(FakeGradleBasedLanguage.id)
    ) {}

    val request = HyperskillOpenStepRequest(step2640.id, FakeGradleBasedLanguage.id)
    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, request)

    assertSame(problemsCourse, project.course)
    assertProblemLoaded(problemsCourse, TOPIC_85_NAME, step2640.title)
  }

  @Test
  fun `test open problem does not reuse regular hyperskill project and creates problems project`() {
    val regularCourse = hyperskillCourseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "stage 1")
        }
      }
    }

    val request = HyperskillOpenStepRequest(step2640.id, FakeGradleBasedLanguage.id)
    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, request).onError { error("Failed to open the problem: $it") }

    // the regular project must not be reused: the problem goes to a newly created problems project
    assertSame(regularCourse, project.course)
    assertNull(regularCourse.getTopicsSection())

    val openedCourse = mockProjectOpener.courseOpenedInNewProject as? HyperskillCourse
                       ?: error("A new problems project should have been created")
    assertEquals(getProblemsProjectName(FakeGradleBasedLanguage.id), openedCourse.name)
    assertNull(openedCourse.hyperskillProject)
    assertProblemLoaded(openedCourse, TOPIC_85_NAME, step2640.title)
  }

  @Test
  fun `test unknown language`() {
    val unknownLanguage = "Unknown language"
    doLanguageValidationTest(unknownLanguage) {
      assertEquals(EduCoreBundle.message("hyperskill.unsupported.language", unknownLanguage), it.message)
    }
  }

  @Test
  fun `test language supported with plugin`() {
    doLanguageValidationTest("python") {
      assertTrue("actual: $it", it is PluginsRequired)
    }
  }

  @Test
  fun `test language not supported in IDE`() {
    val unsupportedLanguage = "Unsupported"
    doLanguageValidationTest(unsupportedLanguage) {
      val expectedMessage = EduCoreBundle.message(
        "rest.service.language.not.supported", ApplicationNamesInfo.getInstance().productName,
        unsupportedLanguage
      )
      assertEquals(expectedMessage, it.message)
    }
  }

  private fun doLanguageValidationTest(language: String, checkError: (CourseValidationResult) -> Unit) {
    mockConnector.configureFromCourse(testRootDisposable, hyperskillCourse(projectId = null) {
      section(HYPERSKILL_TOPICS) {
        lesson(TOPIC_NAME) {
          codeTask(stepId = 4) {
            taskFile("task.txt", "file text")
          }
        }
      }
    })

    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepRequest(4, language)).onError {
      checkError(it)
      return
    }

    error("Error is expected: project shouldn't open")
  }

  private fun assertProblemLoaded(course: HyperskillCourse, topicName: String, problemName: String) {
    val task = course.getTopicsSection()?.getLesson(topicName)?.getTask(problemName)
    assertNotNull("Failed to find `$problemName` problem in `$topicName` topic", task)
  }

  private fun assertStepSourceRequestedOnce(stepInfo: StepInfo) {
    assertEquals("Step source should be loaded once", 1, stepSourceRequestCounts[stepInfo.id] ?: 0)
  }

  private fun configureMockResponsesForProblems() {
    requestedInformation.forEach { information ->
      mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
        if (request.pathWithoutPrams.endsWith(information.path) && request.hasParams(information.param)) {
          if (information is StepInfo) {
            stepSourceRequestCounts[information.id] = (stepSourceRequestCounts[information.id] ?: 0) + 1
          }
          mockResponse(information.file)
        }
        else null
      }
    }
  }

  companion object {
    private const val TOPIC_NAME = "topicName"
    private const val TOPIC_85_NAME = "Wildcards"

    private val step2640 = StepInfo(2640, "Packing bakeries")
    private val step2641 = StepInfo(2641, "List multiplicator")

    @Suppress("unused") // not recommended step
    private val step9886 = StepInfo(9886, "Pets in boxes")
    private val topic85 = TopicInfo(85)

    private val step10960 = StepInfo(10960, "Web calculator")
    private val topic515 = TopicInfo(515)

    @Suppress("unused") // not recommended step
    private val step8146 = StepInfo(8146, "Acronym")
    private val step14259 = StepInfo(14259, "Summer")
    private val topic632 = TopicInfo(632)

    private val step12164 = StepInfo(12164, "The shape of a data frame")
    private val topic1034 = TopicInfo(1034)

    private val requestedInformation = listOf(
      step2640, step2641, topic85,
      step10960, topic515,
      step14259, topic632,
      step12164, topic1034
    )
  }
}
