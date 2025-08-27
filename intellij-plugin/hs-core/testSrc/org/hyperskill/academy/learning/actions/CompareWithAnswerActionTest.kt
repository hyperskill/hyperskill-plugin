package org.hyperskill.academy.learning.actions

import org.hyperskill.academy.learning.EduActionTestCase
import org.hyperskill.academy.learning.EduBrowser
import org.hyperskill.academy.learning.MockEduBrowser
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.stepik.hyperskill.HYPERSKILL_SOLUTIONS_ANCHOR
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillCourseWithFiles
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillTaskLink
import org.hyperskill.academy.learning.testAction
import org.junit.Test

class CompareWithAnswerActionTest : EduActionTestCase() {

  @Test
  fun `test for Hyperskill course`() {
    hyperskillCourseWithFiles {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("task.txt", "stage 1")
        }
        eduTask("task2", stepId = 2) {
          taskFile("task.txt", "stage 2")
        }
      }
    }
    openFirstTaskFile()

    testAction(CompareWithAnswerAction.ACTION_ID)

    val mockEduBrowser = EduBrowser.getInstance() as MockEduBrowser
    assertEquals("${hyperskillTaskLink(findTask(0, 0))}$HYPERSKILL_SOLUTIONS_ANCHOR", mockEduBrowser.lastVisitedUrl)
  }

  private fun openFirstTaskFile() {
    val task = findTask(0, 0).apply { status = CheckStatus.Solved }
    task.openTaskFileInEditor("task.txt")
  }
}