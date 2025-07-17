package com.jetbrains.edu.go.courseGeneration

import com.goide.GoLanguage
import com.goide.sdk.GoSdk
import com.jetbrains.edu.go.GoProjectSettings
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseGeneration.CourseGenerationTestBase
import com.jetbrains.edu.learning.fileTree
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
