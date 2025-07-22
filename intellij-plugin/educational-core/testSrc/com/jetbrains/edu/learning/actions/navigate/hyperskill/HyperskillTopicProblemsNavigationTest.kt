package org.hyperskill.academy.learning.actions.navigate.hyperskill

import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.actions.PreviousTaskAction
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.junit.Test

class HyperskillTopicProblemsNavigationTest : HyperskillNavigateInCourseTestBase() {
  override val course: HyperskillCourse
    get() = createHyperskillCourse(withTopicProblems = true)

  override fun getFirstProblemsTask(): Task = findTopicProblem(topic1LessonName, theoryTaskName)

  @Test
  fun `test navigate to next available on first problems task`() =
    checkNavigationAction(getFirstProblemsTask(), NextTaskAction.ACTION_ID, true)

  @Test
  fun `test navigate to next available on last problems task of first topic`() {
    val problem = findTopicProblem(topic1LessonName, problem2Name)
    checkNavigationAction(problem, NextTaskAction.ACTION_ID, true)
  }

  @Test
  fun `test navigate to previous unavailable on first problems task of second topic`() {
    val problem = findTopicProblem(topic2LessonName, theoryTaskName)
    checkNavigationAction(problem, PreviousTaskAction.ACTION_ID, false)
  }
}