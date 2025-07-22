package org.hyperskill.academy.learning.checker

import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.configuration.PlainTextTaskCheckerProvider.Companion.CHECK_RESULT_FILE
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.allTasks
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.OutputTask
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.document
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings
import org.junit.Test

class PlainTextCheckerTest : CheckersTestBase<EmptyProjectSettings>() {

  override fun createCheckerFixture(): EduCheckerFixture<EmptyProjectSettings> = PlaintTextCheckerFixture()

  override fun createCourse(): Course {
    return course {
      lesson {
        outputTask("OutputTask") {
          taskFile(CHECK_RESULT_FILE) {
            withText("OK!\n")
          }
          dir("tests") {
            taskFile("output.txt") {
              withText("OK!\n")
            }
          }
        }
        outputTask("OutputTaskWithWindowsLineSeparators") {
          taskFile(CHECK_RESULT_FILE) {
            withText("OK!\n")
          }
          taskFile("output.txt") {
            withText("OK!\r\n")
          }
        }
        eduTask("EduTask") {
          taskFile("task.txt") {
            withText("task file")
          }
          taskFile(CHECK_RESULT_FILE) {
            withText("Solved Congratulations!")
          }
          dir("tests") {
            taskFile("Tests.txt") {
              withText(EDU_TEST_FILE_TEXT)
            }
          }
        }
        eduTask("EduTaskWithVisibleTests") {
          taskFile("task.txt") {
            withText("task file")
          }
          taskFile(CHECK_RESULT_FILE) {
            withText("Solved Congratulations!")
          }
          dir("tests") {
            taskFile("Tests.txt", visible = true) {
              withText(EDU_TEST_FILE_TEXT)
            }
          }
        }
      }
    }
  }

  @Test
  fun `test course`() {
    CheckActionListener.expectedMessage { task ->
      when (task) {
        is OutputTask, is EduTask -> CheckUtils.CONGRATULATIONS
        is TheoryTask -> ""
        else -> null
      }
    }
    doTest()
  }

  @Test
  fun `test visible test files content is not change in edu task`() {
    val eduTask = myCourse.allTasks.first { it.name == "EduTaskWithVisibleTests" }

    val taskDir = eduTask.getDir(project.courseDir) ?: error("No task dir found")
    val testFile = eduTask.taskFiles.values.single { EduUtilsKt.isTestsFile(eduTask, it.name) }
    val vTestFile = taskDir.findFileByRelativePath(testFile.name) ?: error("no virtual file found for the test file")

    assertEquals(EDU_TEST_FILE_TEXT, vTestFile.document.text)
    checkTask(eduTask)
    assertEquals(EDU_TEST_FILE_TEXT, vTestFile.document.text)
  }

  companion object {
    private const val EDU_TEST_FILE_TEXT = "test file text"
  }
}
