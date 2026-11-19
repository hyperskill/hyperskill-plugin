package org.hyperskill.academy.learning.courseFormat.tasks

import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTaskType
import java.util.*

/**
 * A Hyperskill step that isn't a code step, answered with the form in the task description: choice, matching, sorting, etc.
 *
 * What the learner sees (options, pairs, rows) belongs to a server-side attempt and differs between attempts, so it is
 * requested from the server every time the task is opened. The course files only store the block type, as the task `type`.
 *
 * Some blocks are answered with task files instead, see [ANSWER_FILES]: the form shows the rest of the step there.
 */
class QuizTask : Task {
  var blockName: String = ""

  /** The attempt the answer belongs to. It comes from the server and is not stored in the course files. */
  var attempt: Attempt? = null

  /** The answer in the format of a submission reply, `null` until the learner gives one. It is not stored in the course files. */
  var reply: Map<String, Any?>? = null

  //used for deserialization
  constructor()

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus, blockName: String) :
    super(name, id, position, updateDate, status) {
    this.blockName = blockName
  }

  override val itemType: String
    get() = blockName

  override val isChangedOnFailed: Boolean
    get() = true

  companion object {
    const val HTML_FILE_NAME: String = "index.html"
    const val CSS_FILE_NAME: String = "style.css"
    const val SQL_FILE_NAME: String = "query.sql"
    const val DATASET_FILE_NAME: String = "dataset.txt"

    /**
     * Blocks that can be answered inside the IDE. `video` is the only block of the website that isn't there: it is a
     * kind of theory, and no step uses it.
     */
    val BLOCK_NAMES: Set<String> = setOf(
      HyperskillTaskType.CHOICE.type,
      HyperskillTaskType.DATASET.type,
      HyperskillTaskType.FILL_BLANKS.type,
      HyperskillTaskType.HTML.type,
      HyperskillTaskType.MATCHING.type,
      HyperskillTaskType.MATH.type,
      HyperskillTaskType.NUMBER.type,
      HyperskillTaskType.PARSONS.type,
      HyperskillTaskType.PROMPT.type,
      HyperskillTaskType.SORTING.type,
      HyperskillTaskType.SQL.type,
      HyperskillTaskType.STRING.type,
      HyperskillTaskType.TABLE.type,
    )

    /**
     * Blocks answered with task files: the learner writes the page or the query in the editor, not in the form.
     */
    val ANSWER_FILES: Map<String, List<String>> = mapOf(
      HyperskillTaskType.HTML.type to listOf(HTML_FILE_NAME, CSS_FILE_NAME),
      HyperskillTaskType.SQL.type to listOf(SQL_FILE_NAME),
    )
  }
}
