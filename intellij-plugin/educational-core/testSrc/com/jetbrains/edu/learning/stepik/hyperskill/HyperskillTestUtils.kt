package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.lang.Language
import org.hyperskill.academy.learning.CourseBuilder
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.authUtils.TokenInfo
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillAccount
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillUserInfo
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.jetbrains.annotations.TestOnly

const val TEST_HYPERSKILL_PROJECT_NAME = "Test Hyperskill Project"

fun testStageName(index: Int): String = "Test Stage $index"

fun logInFakeHyperskillUser() {
  val fakeToken = TokenInfo().apply { accessToken = "faketoken" }
  HyperskillSettings.INSTANCE.account = HyperskillAccount().apply {
    userInfo = HyperskillUserInfo()
    userInfo.id = 1
    saveTokens(fakeToken)
  }
}

fun logOutFakeHyperskillUser() {
  HyperskillSettings.INSTANCE.account = null
}

fun EduTestCase.hyperskillCourseWithFiles(
  projectId: Int? = 1,
  name: String = TEST_HYPERSKILL_PROJECT_NAME,
  language: Language = FakeGradleBasedLanguage,
  completeStages: Boolean = false,
  buildCourse: CourseBuilder.() -> Unit
): HyperskillCourse {
  val course = courseWithFiles(
    name = name, courseProducer = ::HyperskillCourse, courseMode = CourseMode.STUDENT, language = language,
    buildCourse = buildCourse
  ) as HyperskillCourse
  course.init(projectId, completeStages)
  return course
}

@TestOnly
fun hyperskillCourse(
  projectId: Int? = 1,
  language: Language = FakeGradleBasedLanguage,
  completeStages: Boolean = false,
  buildCourse: CourseBuilder.() -> Unit
): HyperskillCourse {
  val course = course(
    name = TEST_HYPERSKILL_PROJECT_NAME,
    courseProducer = ::HyperskillCourse,
    language = language,
    buildCourse = buildCourse
  ) as HyperskillCourse
  course.init(projectId, completeStages)
  return course
}

@Suppress("unused") // want this method to be available only in EduTestCase
fun EduTestCase.defaultHyperskillCourse(): HyperskillCourse {
  return hyperskillCourse {
    frameworkLesson {
      eduTask("task1", stepId = 1) {
        taskFile("src/Task.kt", "stage 1")
        taskFile("test/Tests1.kt", "stage 1 test")
      }
      eduTask("task2", stepId = 2) {
        taskFile("src/Task.kt", "stage 2")
        taskFile("test/Tests2.kt", "stage 2 test")
      }
    }
  }
}

internal fun HyperskillCourse.init(projectId: Int?, completeStages: Boolean) {
  if (projectId == null) {
    return
  }
  hyperskillProject = HyperskillProject().apply {
    id = projectId
    language = FakeGradleBasedLanguage.id
    title = TEST_HYPERSKILL_PROJECT_NAME
  }
  val projectLesson = getProjectLesson()
  if (projectLesson != null) {
    stages = projectLesson.items.mapIndexed { i, task ->
      HyperskillStage(i + 1, testStageName(i + 1), task.id, isStageCompleted = completeStages)
    }
  }
  init(this, false)
}
