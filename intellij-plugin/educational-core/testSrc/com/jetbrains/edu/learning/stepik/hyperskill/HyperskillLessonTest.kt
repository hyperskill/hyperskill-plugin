package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.runWriteAction
import com.intellij.util.ui.UIUtil
import com.jetbrains.edu.coursecreator.AdditionalFilesUtils
import com.jetbrains.edu.learning.*
import com.jetbrains.edu.learning.courseFormat.CourseMode
import com.jetbrains.edu.learning.courseFormat.InMemoryBinaryContents
import com.jetbrains.edu.learning.courseFormat.InMemoryTextualContents
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.stepik.StepikTestUtils
import com.jetbrains.edu.learning.stepik.StepikTestUtils.logOutFakeStepikUser
import com.jetbrains.edu.learning.stepik.api.MockStepikConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.MockHyperskillConnector
import org.apache.commons.codec.binary.Base64
import org.junit.Test

class HyperskillLessonTest : EduTestCase() {
  @Test
  fun `test collecting course additional files`() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse, courseMode = CourseMode.EDUCATOR) {
      frameworkLesson {
        eduTask {}
      }
      additionalFile("package.json", InMemoryTextualContents("My cool dependencies"))
    }
    val info = AdditionalFilesUtils.collectAdditionalLessonInfo(course.lessons.first(), project)

    assertEquals(1, info.additionalFiles.size)
    assertEquals("package.json", info.additionalFiles[0].name)
    assertContentsEqual(
      "package.json",
      InMemoryTextualContents("My cool dependencies"),
      info.additionalFiles[0].contents
    )
  }

  @Test
  fun `test push course with binary file`() {
    StepikTestUtils.loginFakeStepikUser()
    val mockConnector = StepikConnector.getInstance() as MockStepikConnector

    mockConnector.withResponseHandler(testRootDisposable) { request, _ ->
      val responseFileName = when (request.pathWithoutPrams) {
        "/api/lessons" -> "lessons_response_278738129.json"
        "/api/step-sources" -> "step-source.json"
        "/api/attachments" -> "attachments_response_278738129.json"
        else -> return@withResponseHandler MockResponseFactory.notFound()
      }
      mockResponse(responseFileName)
    }
    val dbFilePath = "database.db"
    val base64Text = "eAErKUpNVTA3ZjA0MDAzMVHITczM08suYTh0o+NNPdt26bgThdosKRdPVXHN/wNVUpSamJKbqldSUcKwosqLb/75qC5OmZAJs9O9Di0I/PoCAJ5FH4E="
    val course = courseWithFiles("Test Course", courseMode = CourseMode.EDUCATOR, id = 1) {
      frameworkLesson("lesson1") {
        eduTask {
          taskFile("taskFile1.txt")
          taskFile(dbFilePath, InMemoryBinaryContents.parseBase64Encoding(base64Text), false)
        }
      }
    }

    val firstLesson = course.lessons.first()
    val task = firstLesson.taskList.first()
    val taskFile = task.taskFiles["database.db"]
    runWriteAction {
      taskFile!!.getVirtualFile(project)!!.setBinaryContent(Base64.decodeBase64("binary file"))
    }

    UIUtil.dispatchAllInvocationEvents()
    PushHyperskillLesson.doPush(firstLesson, project)
    logOutFakeStepikUser()
  }

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