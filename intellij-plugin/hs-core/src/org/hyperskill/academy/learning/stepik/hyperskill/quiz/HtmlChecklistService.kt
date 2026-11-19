package org.hyperskill.academy.learning.stepik.hyperskill.quiz

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask

/**
 * Runs the checklist of an `html` step. The checklist needs a browser to render the page in, so the quiz form of the
 * task description runs it, see `hyperskill_html_tests.js`.
 */
fun interface HtmlChecklistRunner {
  /**
   * @return the score of the page made of [sources] (`{html, css}`), or `null` if the checklist can't be run now:
   * the form of the [task] isn't shown, or it didn't answer in time
   */
  fun score(task: QuizTask, sources: Map<String, String>): Any?
}

@Service(Service.Level.PROJECT)
class HtmlChecklistService {
  /** The form shown in the task description, if the description is shown with JCEF. */
  @Volatile
  var runner: HtmlChecklistRunner? = null

  companion object {
    fun getInstance(project: Project): HtmlChecklistService = project.service()
  }
}
