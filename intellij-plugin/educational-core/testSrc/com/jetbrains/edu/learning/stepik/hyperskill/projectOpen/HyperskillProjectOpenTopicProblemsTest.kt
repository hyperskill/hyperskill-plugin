package org.hyperskill.academy.learning.stepik.hyperskill.projectOpen

import com.intellij.openapi.application.ApplicationNamesInfo
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.ext.CourseValidationResult
import org.hyperskill.academy.learning.courseFormat.ext.PluginsRequired
import org.hyperskill.academy.learning.hasParams
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.pathWithoutPrams
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepRequest
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepWithProjectRequest
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillCourse
import org.hyperskill.academy.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.StepInfo
import org.hyperskill.academy.learning.stepik.hyperskill.projectOpen.HyperskillProjectOpenerTestBase.Companion.TopicInfo
import org.junit.Test


class HyperskillProjectOpenTopicProblemsTest : HyperskillProjectOpenerTestBase() {

  override fun setUp() {
    super.setUp()
    configureMockResponsesForStages()
    configureMockResponsesForProblems()
  }

  @Test
  fun `test open non dataset problem with language chosen by user`() {
    val request = HyperskillOpenStepWithProjectRequest(1, step10960.id, "TEXT", true)
    assertThrows(IllegalStateException::class.java) {
      mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, request)
    }
  }

  @Test
  fun `test open non dataset problem with language chosen by user without selected project`() {
    val request = HyperskillOpenStepRequest(step10960.id, FakeGradleBasedLanguage.id, true)
    assertThrows(IllegalStateException::class.java) {
      mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, request)
    }
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

    mockProjectOpener.open(HyperskillOpenInIdeRequestHandler, HyperskillOpenStepWithProjectRequest(1, 4, language)).onError {
      checkError(it)
      return
    }

    error("Error is expected: project shouldn't open")
  }

  private fun configureMockResponsesForProblems() {
    requestedInformation.forEach { information ->
      mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
        if (request.pathWithoutPrams.endsWith(information.path) && request.hasParams(information.param)) {
          mockResponse(information.file)
        }
        else null
      }
    }
  }

  companion object {
    private const val TOPIC_NAME = "topicName"

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