package org.hyperskill.academy.learning.actions.navigate.hyperskill

import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.actions.PreviousTaskAction
import org.hyperskill.academy.learning.actions.navigate.NavigationTestBase
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROBLEMS
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.findTask
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillCourseWithFiles
import org.hyperskill.academy.learning.testAction
import org.junit.Test

abstract class HyperskillNavigateInCourseTestBase : NavigationTestBase() {
  private val stage1Name = "stage1"
  private val stagesLessonName = "lesson"
  protected val problem1Name = "problem1"
  protected val problem2Name = "problem2"
  protected val topic1LessonName = "topic1"
  protected val topic2LessonName = "topic2"
  protected val theoryTaskName = "Theory"

  abstract val course: HyperskillCourse

  abstract fun getFirstProblemsTask(): Task

  @Test
  fun `test navigate to next unavailable on last stage`() {
    val task = course.findTask(stagesLessonName, stage1Name)
    return checkNavigationAction(task, NextTaskAction.ACTION_ID, false)
  }

  @Test
  fun `test navigate to previous unavailable on first problems task`() =
    checkNavigationAction(getFirstProblemsTask(), PreviousTaskAction.ACTION_ID, false)

  protected fun findTopicProblem(lessonName: String, problemName: String): Task {
    return course.getTopicsSection()?.getLesson(lessonName)?.getTask(problemName)
           ?: error("Can't find task from topics section")
  }

  protected fun createHyperskillCourse(withLegacyProblems: Boolean = false, withTopicProblems: Boolean = false) =
    hyperskillCourseWithFiles(completeStages = true) {
      frameworkLesson(stagesLessonName) {
        eduTask(stage1Name) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile("src/Baz.kt", "fun baz() {}")
          taskFile("test/Tests1.kt", "fun tests1() {}")
        }
      }
      if (withLegacyProblems) {
        lesson(HYPERSKILL_PROBLEMS) {
          eduTask(problem1Name) {
            taskFile("src/Task.kt", "fun foo() {}")
          }
          eduTask(problem2Name) {
            taskFile("src/Task.kt", "fun foo() {}")
          }
        }
      }
      if (withTopicProblems) {
        section(HYPERSKILL_TOPICS) {
          lesson(topic1LessonName) {
            theoryTask(theoryTaskName) {
              taskFile("src/Task.kt", "file text")
              taskFile("task.html", "file text")
            }
            eduTask(problem1Name) {
              taskFile("src/Task.kt", "fun foo() {}")
            }
            eduTask(problem2Name) {
              taskFile("src/Task.kt", "fun foo() {}")
            }
          }
          lesson(topic2LessonName) {
            theoryTask(theoryTaskName) {
              taskFile("src/Task.kt", "file text")
              taskFile("task.html", "file text")
            }
            eduTask(problem1Name) {
              taskFile("src/Task.kt", "fun foo() {}")
            }
            eduTask(problem2Name) {
              taskFile("src/Task.kt", "fun foo() {}")
            }
          }
        }
      }
    }

  protected fun checkNavigationAction(task: Task, actionId: String, shouldBeEnabled: Boolean) {
    val firstTask = task.lesson.taskList.first()
    NavigationUtils.navigateToTask(project, task, firstTask, false)
    task.openTaskFileInEditor("src/Task.kt")
    testAction(actionId, shouldBeEnabled = shouldBeEnabled, shouldBeVisible = true)
  }
}