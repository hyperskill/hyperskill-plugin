package org.hyperskill.academy.learning.stepik.hyperskill.quiz

import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.attempts.Dataset
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.stepik.api.QuizReply
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.hyperskill.academy.learning.stepik.hyperskill.stepLink

/**
 * What the quiz form shows for a task, see `hyperskill_quiz.js`.
 */
class QuizFormState(
  val blockName: String,
  val stepLink: String,
  val dataset: Dataset? = null,
  val reply: Map<String, Any?>? = null,
  val isDisabled: Boolean = false,
  val error: String? = null
) {
  fun toMap(): Map<String, Any?> = mapOf(
    "blockName" to blockName,
    "stepLink" to stepLink,
    "dataset" to dataset,
    "reply" to reply,
    "disabled" to isDisabled,
    "error" to error
  )
}

/**
 * Requests the attempt to solve and prepares the [task] to be checked. Accesses the network.
 *
 * A checked quiz shows the attempt of the last submission with the given answer. A solved one stays like that, and a
 * failed one until the learner retries it. An unchecked quiz continues the active attempt, as a new one would shuffle
 * the options and make the server treat the previous attempt as outdated, so a new one is created only if there is none.
 */
fun loadQuizFormState(task: QuizTask): QuizFormState {
  fun failed(error: String) = QuizFormState(task.blockName, stepLink(task.id), error = error)

  HyperskillSettings.INSTANCE.account ?: return failed(EduCoreBundle.message("check.login.error", EduNames.JBA))

  val connector = HyperskillConnector.getInstance()
  val attempts = connector.getAttempts(task.id).onError { return failed(it) }
  val lastSubmission = connector.getSubmissions(task.id).maxByOrNull { it.id ?: 0 }

  val isChecked = task.status != CheckStatus.Unchecked
  val checkedAttempt = if (isChecked) attempts.find { it.id == lastSubmission?.attempt } else null
  val attempt: Attempt = checkedAttempt
                         ?: attempts.find { it.isActive && it.isRunning }
                         ?: connector.postAttempt(task).onError { return failed(it) }
  val reply = (lastSubmission?.takeIf { it.attempt == attempt.id }?.reply as? QuizReply)?.fields

  task.attempt = attempt
  task.reply = reply
  return QuizFormState(task.blockName, stepLink(task.id), attempt.dataset, reply, isDisabled = isChecked)
}
