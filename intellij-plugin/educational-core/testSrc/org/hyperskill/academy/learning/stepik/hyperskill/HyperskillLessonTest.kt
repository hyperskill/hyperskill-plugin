package org.hyperskill.academy.learning.stepik.hyperskill

import org.hyperskill.academy.learning.EduSettings
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.MockResponseFactory
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.pathWithoutPrams
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.MockHyperskillConnector
import org.junit.Test

class HyperskillLessonTest : EduTestCase() {

  @Test
  fun `test getting lesson does not override task titles`() {
    val mockConnector = HyperskillConnector.getInstance() as MockHyperskillConnector

    val course = hyperskillCourse {
      frameworkLesson {
        eduTask {}
      }
    }

    course.stages = listOf(HyperskillStage(stageId = 1, stageTitle = "stage1", stageStepId = 3885098))

    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      val responseFileName = when (request.pathWithoutPrams) {
        "/api/steps" -> "steps_response_3885098.json"
        "/api/project/${course.hyperskillProject?.id}/additional-files/additional-files.json" -> "attachments_response_278738129.json"
        else -> return@withResponseHandler MockResponseFactory.notFound()
      }
      mockResponse(responseFileName)
    }

    val lesson = mockConnector.getLesson(course)!!
    assertEquals(1, lesson.taskList.size)
    assertEquals("stage1", lesson.taskList[0].name)
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "/stepik/hyperskill/"

  override fun tearDown() {
    try {
      EduSettings.getInstance().user = null
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}