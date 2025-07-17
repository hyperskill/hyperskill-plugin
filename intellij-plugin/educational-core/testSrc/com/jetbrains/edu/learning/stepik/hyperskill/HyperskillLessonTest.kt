package com.jetbrains.edu.learning.stepik.hyperskill

import com.jetbrains.edu.learning.EduSettings
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.MockResponseFactory
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.pathWithoutPrams
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
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