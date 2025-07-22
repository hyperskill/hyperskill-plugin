package org.hyperskill.academy.learning.stepik.hyperskill.checker

import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.checker.*
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings
import org.hyperskill.academy.learning.stepik.hyperskill.markStageAsCompleted
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.junit.Test

class HyperskillPlainTextCheckerTest : CheckersTestBase<EmptyProjectSettings>() {

  override fun createCheckerFixture(): EduCheckerFixture<EmptyProjectSettings> = PlaintTextCheckerFixture()

  override fun createCourse(): Course {
    val course = course(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask(stepId = 1) {
          checkResultFile(CheckStatus.Solved)
        }
        eduTask(stepId = 2) {
          checkResultFile(CheckStatus.Solved)
        }
      }
    } as HyperskillCourse
    course.stages = listOf(HyperskillStage(1, "", 1), HyperskillStage(2, "", 2))
    course.hyperskillProject = HyperskillProject()
    return course
  }

  @Test
  fun `test course`() {
    CheckActionListener.expectedMessage { task ->
      when (task.index) {
        1 -> CheckUtils.CONGRATULATIONS
        2 -> EduCoreBundle.message("hyperskill.next.project", HYPERSKILL_PROJECTS_URL)
        else -> null
      }
    }
    TaskToolWindowView.getInstance(project).currentTask = project.getCurrentTask()
    doTest()
  }

  override fun checkTask(task: Task): List<AssertionError> {
    val assertions = super.checkTask(task)
    if (assertions.isEmpty() && task.status == CheckStatus.Solved) {
      markStageAsCompleted(task)
    }
    return assertions
  }
}
