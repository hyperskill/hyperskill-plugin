package org.hyperskill.academy.go.courseGeneration

import com.goide.GoLanguage
import com.goide.sdk.GoSdk
import org.hyperskill.academy.go.GoProjectSettings
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase
import org.hyperskill.academy.learning.fileTree
import org.junit.Test

class GoCourseBuilderTest : CourseGenerationTestBase<GoProjectSettings>() {
  override val defaultSettings: GoProjectSettings = GoProjectSettings(GoSdk.NULL)

  @Test
  fun `test study course structure`() {
    val course = course(language = GoLanguage.INSTANCE) {
      lesson {
        eduTask {
          taskFile("main/main.go")
          taskFile("test/task_test.go")
          taskFile("task.go")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("main") {
          file("main.go")
        }
        dir("test") {
          file("task_test.go")
        }
        file("task.go")
        file("task.html")
      }
    }.assertEquals(rootDir)
  }
}
