package org.hyperskill.academy.socialMedia

import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.checker.CheckListener
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillCourseWithFiles
import org.hyperskill.academy.socialMedia.suggestToPostDialog.SuggestToPostDialogUI
import org.hyperskill.academy.socialMedia.suggestToPostDialog.withMockSuggestToPostDialogUI
import org.junit.Test

class SuggestToPostOnProjectCompletionListenerTest : EduTestCase() {

  override fun setUp() {
    super.setUp()
    SocialMediaSettings.getInstance().askToPost = true
  }

  override fun tearDown() {
    try {
      SocialMediaSettings.getInstance().askToPost = false
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  @Test
  fun `test show dialog after last project task solved`() {
    val course = createHyperskillCourse()
    val projectLesson = course.getProjectLesson() ?: error("No project lesson")
    val lastTask = projectLesson.getTask("Task2") ?: error("No Task2")
    // All other project tasks are already solved before the last one is checked
    projectLesson.visitTasks { if (it != lastTask) it.status = CheckStatus.Solved }

    val shown = simulateCheck(lastTask) { it.status = CheckStatus.Solved }
    assertTrue("Dialog should be shown after the whole project is solved", shown)
  }

  @Test
  fun `test do not show dialog if not all project tasks solved`() {
    val course = createHyperskillCourse()
    val lastTask = course.getProjectLesson()?.getTask("Task2") ?: error("No Task2")
    // `Task1` stays unchecked

    val shown = simulateCheck(lastTask) { it.status = CheckStatus.Solved }
    assertFalse("Dialog should not be shown until the whole project is solved", shown)
  }

  @Test
  fun `test do not show dialog if solved task is not a project task`() {
    val course = createHyperskillCourse()
    val codeTask = course.getLesson(HYPERSKILL_TOPICS, TOPIC_NAME)?.getTask("CodeTask") ?: error("No CodeTask")

    val shown = simulateCheck(codeTask) { it.status = CheckStatus.Solved }
    assertFalse("Dialog should not be shown for tasks outside the project lesson", shown)
  }

  @Test
  fun `test do not show dialog if project task solved again`() {
    val course = createHyperskillCourse()
    val projectLesson = course.getProjectLesson() ?: error("No project lesson")
    projectLesson.visitTasks { it.status = CheckStatus.Solved }
    val lastTask = projectLesson.getTask("Task2") ?: error("No Task2")

    // The task is already solved before the check, so re-solving must not show the dialog
    val shown = simulateCheck(lastTask) { it.status = CheckStatus.Solved }
    assertFalse("Dialog should not be shown when an already solved task is re-checked", shown)
  }

  @Test
  fun `test do not show dialog if disabled in settings`() {
    SocialMediaSettings.getInstance().askToPost = false
    val course = createHyperskillCourse()
    val projectLesson = course.getProjectLesson() ?: error("No project lesson")
    val lastTask = projectLesson.getTask("Task2") ?: error("No Task2")
    projectLesson.visitTasks { if (it != lastTask) it.status = CheckStatus.Solved }

    val shown = simulateCheck(lastTask) { it.status = CheckStatus.Solved }
    assertFalse("Dialog should not be shown when the suggestion is disabled in settings", shown)
  }

  /**
   * Drives the real (EP-registered) listener through a check of [task]:
   * captures the status before the check, applies [solve], then fires `afterCheck`.
   * Returns whether the suggestion dialog would be shown.
   */
  private fun simulateCheck(task: Task, solve: (Task) -> Unit): Boolean {
    val listener = CheckListener.EP_NAME.findExtensionOrFail(SuggestToPostOnProjectCompletionListener::class.java)
    var shown = false
    withMockSuggestToPostDialogUI(object : SuggestToPostDialogUI {
      override fun showAndGet(): Boolean {
        shown = true
        return false
      }
    }) {
      listener.beforeCheck(project, task)
      solve(task)
      listener.afterCheck(project, task, CheckResult.SOLVED)
    }
    return shown
  }

  private fun createHyperskillCourse(): HyperskillCourse = hyperskillCourseWithFiles {
    frameworkLesson("Project") {
      eduTask("Task1") {
        taskFile("task.txt")
      }
      eduTask("Task2") {
        taskFile("task.txt")
      }
    }
    section(HYPERSKILL_TOPICS) {
      lesson(TOPIC_NAME) {
        codeTask("CodeTask") {
          taskFile("task.txt")
        }
      }
    }
  }

  companion object {
    private const val TOPIC_NAME = "topicName"
  }
}
