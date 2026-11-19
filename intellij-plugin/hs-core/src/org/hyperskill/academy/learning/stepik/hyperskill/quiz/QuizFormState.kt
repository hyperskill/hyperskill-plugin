package org.hyperskill.academy.learning.stepik.hyperskill.quiz

import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.attempts.Dataset
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTaskType
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.stepik.CHECKLIST
import org.hyperskill.academy.learning.stepik.DatasetStepOptions
import org.hyperskill.academy.learning.stepik.HtmlStepOptions
import org.hyperskill.academy.learning.stepik.TIME_LIMIT
import org.hyperskill.academy.learning.stepik.api.QuizReply
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.hyperskill.academy.learning.stepik.hyperskill.stepLink

/**
 * What the quiz form shows for a task, see `hyperskill_quiz.js`.
 *
 * @param options what the form needs from the step itself: the checklist of an `html` step, the time limit of a `dataset` one
 * @param timeLeft seconds left to answer within the attempt, if it is limited in time
 * @param sources the HTML and CSS of an `html` step, as the learner has them in the task files
 */
class QuizFormState(
  val blockName: String,
  val stepLink: String,
  val dataset: Dataset? = null,
  val reply: Map<String, Any?>? = null,
  val isDisabled: Boolean = false,
  val error: String? = null,
  val hasAttempt: Boolean = false,
  val timeLeft: Long? = null,
  val options: Map<String, Any?> = emptyMap(),
  val sources: Map<String, String>? = null
) {
  fun withSources(sources: Map<String, String>): QuizFormState =
    QuizFormState(blockName, stepLink, dataset, reply, isDisabled, error, hasAttempt, timeLeft, options, sources)

  fun toMap(): Map<String, Any?> = mapOf(
    "blockName" to blockName,
    "stepLink" to stepLink,
    "dataset" to dataset,
    "reply" to reply,
    "disabled" to isDisabled,
    "error" to error,
    "hasAttempt" to hasAttempt,
    "timeLeft" to timeLeft,
    "options" to options,
    "sources" to sources
  )
}

/**
 * Requests the attempt to solve and prepares the [task] to be checked. Accesses the network.
 *
 * A checked quiz shows the attempt of the last submission with the given answer. A solved one stays like that, and a
 * failed one until the learner retries it. An unchecked quiz continues the active attempt, as a new one would shuffle
 * the options and make the server treat the previous attempt as outdated, so a new one is created only if there is none.
 *
 * A `dataset` step is an exception: its attempt starts the timer, so it is created only when the learner downloads the
 * dataset, see [downloadDataset].
 */
fun loadQuizFormState(task: QuizTask): QuizFormState {
  fun failed(error: String) = QuizFormState(task.blockName, stepLink(task.id), error = error)

  HyperskillSettings.INSTANCE.account ?: return failed(EduCoreBundle.message("check.login.error", EduNames.JBA))

  val connector = HyperskillConnector.getInstance()
  val attempts = connector.getAttempts(task.id).onError { return failed(it) }
  val lastSubmission = connector.getSubmissions(task.id).maxByOrNull { it.id ?: 0 }

  val isChecked = task.status != CheckStatus.Unchecked
  val checkedAttempt = if (isChecked) attempts.find { it.id == lastSubmission?.attempt } else null
  val attempt: Attempt? = when {
    checkedAttempt != null -> checkedAttempt
    task.isDataset -> attempts.find { it.isActive }
    else -> attempts.find { it.isActive && it.isRunning } ?: connector.postAttempt(task).onError { return failed(it) }
  }
  val reply = (lastSubmission?.takeIf { attempt != null && it.attempt == attempt.id }?.reply as? QuizReply)?.fields
  val options = loadStepOptions(task).onError { return failed(it) }

  task.attempt = attempt
  task.reply = reply
  return QuizFormState(
    task.blockName,
    stepLink(task.id),
    attempt?.dataset,
    reply,
    isDisabled = isChecked,
    hasAttempt = attempt != null,
    timeLeft = attempt?.timeLeft,
    options = options
  )
}

private fun loadStepOptions(task: QuizTask): Result<Map<String, Any?>, String> {
  if (task.blockName != HyperskillTaskType.HTML.type && !task.isDataset) return Ok(emptyMap())

  val stepSource = HyperskillConnector.getInstance().getStepSource(task.id).onError { return Err(it) }
  val options = when (val stepOptions = stepSource.block?.options) {
    is HtmlStepOptions -> mapOf(CHECKLIST to stepOptions.checklist.orEmpty())
    is DatasetStepOptions -> mapOf(TIME_LIMIT to stepOptions.timeLimit)
    else -> emptyMap()
  }
  return Ok(options)
}

val QuizTask.isDataset: Boolean
  get() = blockName == HyperskillTaskType.DATASET.type
