package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.application.runWriteActionAndWait
import com.intellij.openapi.vfs.VfsUtil
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.getActionById
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.MockHyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.hyperskill.academy.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import org.hyperskill.academy.learning.stepik.hyperskill.update.SyncHyperskillCourseAction
import org.hyperskill.academy.learning.update.CourseUpdateCheckerTestBase
import org.junit.Test
import java.util.*
import kotlin.test.assertNotEquals

class HyperskillCourseUpdateCheckerTest : CourseUpdateCheckerTestBase() {

  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  private fun configureResponse(stagesResponse: String, courseResponse: String = "course_response.json") {
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      ADDITIONAL_FILES_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      mockResponse("attachments.json")
    }
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      COURSES_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      mockResponse(courseResponse)
    }
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      STAGES_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      mockResponse(stagesResponse)
    }
    mockConnector.withResponseHandler(testRootDisposable) { _, path ->
      STEPS_REQUEST_RE.matchEntire(path) ?: return@withResponseHandler null
      mockResponse("steps_response_111.json")
    }
  }

  @Test
  fun `test check scheduled for upToDate course`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse()
    doTest(HyperskillCourseUpdateChecker.getInstance(project), true, 1, 2) {}
  }

  @Test
  fun `test check scheduled for newly created course`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse(true)
    doTest(HyperskillCourseUpdateChecker.getInstance(project), true, 0, 1) {}
  }

  @Test
  fun `test no isUpToDate check for newly created course at project opening`() {
    configureResponse("stages_empty_response.json")
    createHyperskillCourse(true)
    testNoCheck(HyperskillCourseUpdateChecker.getInstance(project))
  }

  @Test
  fun `test check scheduled for not upToDate course with notification`() {
    configureResponse("stages_response.json")
    val course = course(courseProducer = ::HyperskillCourse) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 111) {
          taskFile("src/Task.kt", "fun foo() {}")
        }
      }
    } as HyperskillCourse

    createCourseStructure(course)
    course.hyperskillProject = HyperskillProject()
    course.updateDate = Date(0)
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, false)
    HyperskillSettings.INSTANCE.updateAutomatically = false

    doTest(HyperskillCourseUpdateChecker.getInstance(project), false, 1, 2, 2) {}
  }

  @Test
  fun `test course updated at sync action`() {
    configureResponse("stages_response.json", courseResponse = "course_response_lite.json")
    val course = createHyperskillCourse(false)
    val lesson = FrameworkLesson()
    lesson.parent = course
    course.addLesson(lesson)
    course.hyperskillProject!!.title = "Outdated title"

    // Make sure the lesson has a name and the corresponding folder
    lesson.name = "lesson-new"
    runWriteActionAndWait {
      VfsUtil.createDirectoryIfMissing(project.courseDir, "lesson-new")
    }

    var notificationShown = false
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(Notifications.TOPIC, object : Notifications {
      override fun notify(notification: Notification) {
        notificationShown = true
        assertEquals(EduCoreBundle.message("update.notification.title"), notification.title)
      }
    })

    synchronizeCourseViaAction()
    assertTrue(notificationShown)
    assertEquals("Phone Book", course.hyperskillProject!!.title)
    assertNotEquals(Date(0), course.updateDate)
  }

  @Test
  fun `test notification shown for up to date course at sync action`() {
    configureResponse("stages_empty_response.json", courseResponse = "course_response_lite.json")
    createHyperskillCourse(true)

    var notificationShown = false
    val connection = project.messageBus.connect(testRootDisposable)
    connection.subscribe(Notifications.TOPIC, object : Notifications {
      override fun notify(notification: Notification) {
        notificationShown = true
        assertEquals(EduCoreBundle.message("update.nothing.to.update"), notification.title)
      }
    })

    synchronizeCourseViaAction()
    assertTrue(notificationShown)
  }

  private fun synchronizeCourseViaAction() {
    val action = getActionById<SyncHyperskillCourseAction>(SyncHyperskillCourseAction.ACTION_ID)
    action.synchronizeCourse(project)
  }

  private fun createHyperskillCourse(isNewlyCreated: Boolean = false): HyperskillCourse {
    val course = course(courseProducer = ::HyperskillCourse) { } as HyperskillCourse
    course.apply {
      id = 1
      hyperskillProject = HyperskillProject().apply {
        id = 1
        title = "Phone Book"
      }
    }

    createCourseStructure(course)
    project.putUserData(CourseProjectGenerator.EDU_PROJECT_CREATED, isNewlyCreated)
    return course
  }

  override fun checkNotification(
    notificationListener: NotificationListener,
    isCourseUpToDate: Boolean,
    notificationText: String
  ) {
    if (isCourseUpToDate) {
      if (notificationListener.notificationShown) {
        assertEquals(
          EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT),
          notificationListener.notificationText
        )
      }
    }
    else {
      assertTrue("Notification wasn't shown", notificationListener.notificationShown)
      assertEquals(notificationText, notificationListener.notificationText)
    }
  }

  override fun getTestDataPath(): String = super.getTestDataPath() + "stepik/hyperskill/"

  override fun tearDown() {
    try {
      HyperskillSettings.INSTANCE.updateAutomatically = true
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  companion object {
    private val COURSES_REQUEST_RE = """/api/projects?.*""".toRegex()
    private val STAGES_REQUEST_RE = """/api/stages?.*""".toRegex()
    private val STEPS_REQUEST_RE = """/api/steps?.*""".toRegex()
    private val ADDITIONAL_FILES_REQUEST_RE = """/api/projects/\d+/additional-files?.*""".toRegex()
  }
}