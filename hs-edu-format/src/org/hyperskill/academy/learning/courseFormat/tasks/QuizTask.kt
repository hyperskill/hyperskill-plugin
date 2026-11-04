package org.hyperskill.academy.learning.courseFormat.tasks

import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTaskType
import java.util.*

/**
 * A Hyperskill step that is answered right in the task description: choice, matching, sorting, etc.
 *
 * What the learner sees (options, pairs, rows) belongs to a server-side attempt and differs between attempts, so it is
 * requested from the server every time the task is opened. The course files only store the block type, as the task `type`.
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
    /**
     * Blocks that can be answered inside the IDE. The other non-code blocks need something only the website has:
     * `dataset` is checked against a downloaded file, `html`, `sql` and `prompt` have their own editors and `video` is a player.
     */
    val BLOCK_NAMES: Set<String> = setOf(
      HyperskillTaskType.CHOICE.type,
      HyperskillTaskType.FILL_BLANKS.type,
      HyperskillTaskType.MATCHING.type,
      HyperskillTaskType.MATH.type,
      HyperskillTaskType.NUMBER.type,
      HyperskillTaskType.PARSONS.type,
      HyperskillTaskType.SORTING.type,
      HyperskillTaskType.STRING.type,
      HyperskillTaskType.TABLE.type,
    )
  }
}
