package org.hyperskill.academy.learning.stepik.hyperskill.courseSelection

import kotlinx.coroutines.runBlocking
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.MockResponseFactory
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.pathWithoutPrams
import org.hyperskill.academy.learning.stepik.hyperskill.api.*
import org.hyperskill.academy.learning.stepik.hyperskill.defaultHyperskillCourse
import org.hyperskill.academy.learning.stepik.hyperskill.logInFakeHyperskillUser
import org.hyperskill.academy.learning.stepik.hyperskill.logOutFakeHyperskillUser
import org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI.HyperskillPlatformProvider
import org.junit.Test

class HyperskillPlatformProviderTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logOutFakeHyperskillUser()
    CoursesStorage.getInstance().state.courses.clear()
  }


  @Test
  fun `test selected project added`() {
    logInFakeHyperskillUser()
    val profile = HyperskillUserInfo().apply { hyperskillProjectId = 1 }
    val hyperskillCourse = defaultHyperskillCourse()

    mockConnector.apply {
      withResponseHandler(testRootDisposable) { request, _ ->
        MockResponseFactory.fromString(
          when (request.pathWithoutPrams) {
            "/api/profiles/current" -> objectMapper.writeValueAsString(ProfilesList().apply { profiles = listOf(profile) })
            "/api/projects/1" -> objectMapper.writeValueAsString(ProjectsList().also {
              it.projects = listOf(hyperskillCourse.hyperskillProject!!)
            })

            else -> return@withResponseHandler null
          }
        )
      }
    }

    val courseGroup = loadCourses().first()

    assertTrue(courseGroup.courses.size == 1)

    val course = courseGroup.courses.first()
    assertTrue(course is HyperskillCourse)
    assertEquals(1, course.id)
  }

  @Test
  fun `test local content added`() {
    val localHyperskillCourse = defaultHyperskillCourse()
    CoursesStorage.getInstance().addCourse(localHyperskillCourse, "", 1, 4)

    val courseGroup = loadCourses().first()
    assertEquals(1, courseGroup.courses.size)
    val courseFromProvider = courseGroup.courses.first()
    assertEquals(localHyperskillCourse.id, courseFromProvider.id)

    CoursesStorage.getInstance().removeCourseByLocation("")
  }

  private fun loadCourses() = runBlocking {
    HyperskillPlatformProvider().loadCourses()
  }
}