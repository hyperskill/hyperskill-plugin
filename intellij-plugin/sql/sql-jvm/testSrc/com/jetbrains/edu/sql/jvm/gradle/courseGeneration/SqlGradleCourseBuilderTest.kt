package org.hyperskill.academy.sql.jvm.gradle.courseGeneration

import com.intellij.sql.psi.SqlLanguage
import org.hyperskill.academy.jvm.courseGeneration.JvmCourseGenerationTestBase
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.fileTree
import org.hyperskill.academy.sql.jvm.gradle.SqlGradleStartupActivity
import org.hyperskill.academy.sql.jvm.gradle.sqlCourse
import org.junit.Test

class SqlGradleCourseBuilderTest : JvmCourseGenerationTestBase() {

  override fun setUp() {
    super.setUp()
    SqlGradleStartupActivity.disable(testRootDisposable)
  }

  @Test
  fun `test study course structure`() {
    val course = sqlCourse {
      lesson {
        eduTask {
          taskFile("src/task.sql")
          taskFile("src/migration.sql")
          taskFile("src/data/data.sql")
          taskFile("test/SqlTest.kt")
        }
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file("task.sql")
          file("migration.sql")
          dir("data") {
            file("data.sql")
          }
        }
        dir("test") {
          file("SqlTest.kt")
        }
        file("task.html")
      }
      file("build.gradle")
      file("settings.gradle")
    }.assertEquals(rootDir)
  }

  @Test
  fun `test student course additional files with kts files`() {
    val newCourse = course(language = SqlLanguage.INSTANCE) {
      additionalFile("build.gradle.kts", "")
      additionalFile("settings.gradle.kts", "")
    }
    createCourseStructure(newCourse)

    assertListOfAdditionalFiles(
      newCourse,
      "build.gradle.kts" to null,
      "settings.gradle.kts" to null
      //build.gradle and settings.gradle are not created
    )
  }
}
