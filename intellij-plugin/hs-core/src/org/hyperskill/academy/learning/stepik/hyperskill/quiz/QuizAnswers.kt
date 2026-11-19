package org.hyperskill.academy.learning.stepik.hyperskill.quiz

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.getText
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTaskType
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import java.io.IOException

/**
 * Whether the task can be answered without the form of the task description, which needs JCEF.
 * Only a `sql` step can: its answer is the query file. An `html` step needs the form to run its checklist.
 */
val QuizTask.isAnsweredWithoutForm: Boolean
  get() = blockName == HyperskillTaskType.SQL.type

/**
 * The HTML and CSS of an `html` step, as the learner has them in the task files, in the format of the website:
 * `{html, css}`. `null` for other steps.
 */
fun readHtmlSources(project: Project, task: QuizTask): Map<String, String>? {
  if (task.blockName != HyperskillTaskType.HTML.type) return null
  return mapOf(
    HTML to task.readAnswerFile(project, QuizTask.HTML_FILE_NAME),
    CSS to task.readAnswerFile(project, QuizTask.CSS_FILE_NAME)
  )
}

/**
 * The reply of a `sql` step: the query from its file, or `null` if there is no query yet.
 */
fun readSqlReply(project: Project, task: QuizTask): Map<String, Any?>? {
  val query = task.readAnswerFile(project, QuizTask.SQL_FILE_NAME)
  if (query.isBlank()) return null
  return mapOf(SOLVE_SQL to query.withTrailingNewLine())
}

/**
 * The reply of an `html` step in the format the server expects: the page, and the score the checklist gives it.
 * The website sends both sources ending with a new line, so the reply does too.
 */
fun toHtmlReply(sources: Map<String, String>, score: Any?): Map<String, Any?> = mapOf(
  HTML to sources[HTML].orEmpty().withTrailingNewLine(),
  CSS to sources[CSS].orEmpty().withTrailingNewLine(),
  SCORE to score
)

private fun QuizTask.readAnswerFile(project: Project, name: String): String = getTaskFile(name)?.getText(project).orEmpty()

private fun String.withTrailingNewLine(): String = if (endsWith("\n")) this else "$this\n"

/**
 * Saves the dataset of a `dataset` [task] into [QuizTask.DATASET_FILE_NAME] next to the task files. Accesses the network.
 *
 * Like on the website, an attempt starts the timer, so it is created here, when the learner asks for the data, and not
 * when the task is opened. The dataset of a running attempt is downloaded again, unless [startNew] asks for a new attempt:
 * the time is over, or the learner wants other data.
 */
fun downloadDataset(project: Project, task: QuizTask, startNew: Boolean): Result<VirtualFile, String> {
  val connector = HyperskillConnector.getInstance()
  val currentAttempt = task.attempt?.takeIf { !startNew && it.timeLeft != 0L }
  val attempt = currentAttempt ?: connector.postAttempt(task).onError { return Err(it) }
  val dataset = connector.getDataset(attempt).onError { return Err(it) }

  val taskDir = task.getDir(project.courseDir) ?: return Err(EduCoreBundle.message("hyperskill.dataset.failed.to.save"))
  val file = try {
    invokeAndWaitIfNeeded {
      runWriteAction {
        val file = taskDir.findChild(QuizTask.DATASET_FILE_NAME) ?: taskDir.createChildData(task, QuizTask.DATASET_FILE_NAME)
        VfsUtil.saveText(file, dataset)
        file
      }
    }
  }
  catch (e: IOException) {
    return Err(e.message ?: EduCoreBundle.message("hyperskill.dataset.failed.to.save"))
  }

  if (attempt !== currentAttempt) {
    task.attempt = attempt
    task.reply = null
  }
  return Ok(file)
}

private const val HTML = "html"
private const val CSS = "css"
private const val SCORE = "score"
private const val SOLVE_SQL = "solve_sql"
