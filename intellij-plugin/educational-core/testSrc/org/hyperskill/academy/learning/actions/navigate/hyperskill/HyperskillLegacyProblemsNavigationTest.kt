package org.hyperskill.academy.learning.actions.navigate.hyperskill

import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROBLEMS
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.findTask
import org.junit.Test

class HyperskillLegacyProblemsNavigationTest : HyperskillNavigateInCourseTestBase() {
  override val course: HyperskillCourse
    get() = createHyperskillCourse(withLegacyProblems = true)

  override fun getFirstProblemsTask(): Task = course.findTask(HYPERSKILL_PROBLEMS, problem1Name)

  @Test
  fun `test navigate to next available on first problem`() =
    checkNavigationAction(getFirstProblemsTask(), NextTaskAction.ACTION_ID, true)

  @Test
  fun `test navigate to next available on last problem`() {
    val secondProblem = course.findTask(HYPERSKILL_PROBLEMS, problem2Name)
    checkNavigationAction(secondProblem, NextTaskAction.ACTION_ID, true)
  }
}