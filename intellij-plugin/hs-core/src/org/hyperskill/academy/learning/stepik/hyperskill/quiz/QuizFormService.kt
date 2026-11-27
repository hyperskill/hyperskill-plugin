package org.hyperskill.academy.learning.stepik.hyperskill.quiz

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask

/**
 * The form of a quiz step shown under its question in the task description, see `hyperskill_quiz.js`.
 */
interface QuizForm {
  /**
   * Runs the checklist of an `html` step. The checklist needs a browser to render the page in, so the form runs it,
   * see `hyperskill_html_tests.js`.
   *
   * @return the score of the page made of [sources] (`{html, css}`), or `null` if the checklist can't be run now:
   * the form of the [task] isn't shown, or it didn't answer in time
   */
  fun score(task: QuizTask, sources: Map<String, String>): Any?

  /**
   * Scrolls the task description to the form of the [task] and puts the focus into it, if the form is shown.
   */
  fun focus(task: QuizTask)
}

@Service(Service.Level.PROJECT)
class QuizFormService {
  /** The form shown in the task description, if the description is shown with JCEF. */
  @Volatile
  var form: QuizForm? = null

  companion object {
    fun getInstance(project: Project): QuizFormService = project.service()
  }
}
