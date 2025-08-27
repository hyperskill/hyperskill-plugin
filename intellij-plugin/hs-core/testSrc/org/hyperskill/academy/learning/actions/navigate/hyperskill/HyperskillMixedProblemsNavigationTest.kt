package org.hyperskill.academy.learning.actions.navigate.hyperskill

import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.actions.PreviousTaskAction
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROBLEMS
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.findTask
import org.junit.Test

class HyperskillMixedProblemsNavigationTest : HyperskillNavigateInCourseTestBase() {
  override val course: HyperskillCourse
    get() = createHyperskillCourse(withLegacyProblems = true, withTopicProblems = true)

  override fun getFirstProblemsTask(): Task = course.findTask(HYPERSKILL_PROBLEMS, problem1Name)

  @Test
  fun `test navigate to next available on last legacy problem`() {
    val problem = course.findTask(HYPERSKILL_PROBLEMS, problem2Name)
    checkNavigationAction(problem, NextTaskAction.ACTION_ID, true)
  }

  @Test
  fun `test navigate to previous unavailable on first topic problems task`() {
    val problem = findTopicProblem(topic1LessonName, theoryTaskName)
    checkNavigationAction(problem, PreviousTaskAction.ACTION_ID, false)
  }
}