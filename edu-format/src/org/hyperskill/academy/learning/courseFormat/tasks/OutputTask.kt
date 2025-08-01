package org.hyperskill.academy.learning.courseFormat.tasks

import org.hyperskill.academy.learning.courseFormat.CheckStatus
import java.util.*

/**
 * Task type that allows to test output without any test files.
 * Correct output is specified in output.txt file which is invisible for student.
 * @see OutputTaskChecker
 */
class OutputTask : OutputTaskBase {
  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override val itemType: String = OUTPUT_TASK_TYPE

  override val isToSubmitToRemote: Boolean
    get() = checkStatus != CheckStatus.Unchecked

  companion object {
    const val OUTPUT_TASK_TYPE: String = "output"
  }
}
