package org.hyperskill.academy.rust.courseGeneration

import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase
import org.hyperskill.academy.learning.fileTree
import org.hyperskill.academy.rust.RsProjectSettings
import org.junit.Test
import org.rust.lang.RsLanguage

class RsCourseBuilderTest : CourseGenerationTestBase<RsProjectSettings>() {

  override val defaultSettings: RsProjectSettings = RsProjectSettings(null)

  @Test
  fun `test study course structure`() {
    val course = course(language = RsLanguage) {
      lesson {
        eduTask {
          taskFile("src/main.rs")
          taskFile("tests/tests.rs")
          taskFile("Cargo.toml")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("main.rs")
        }
        dir("tests") {
          file("tests.rs")
        }
        file("task.md")
        file("Cargo.toml")
      }
    }.assertEquals(rootDir)
  }
}
