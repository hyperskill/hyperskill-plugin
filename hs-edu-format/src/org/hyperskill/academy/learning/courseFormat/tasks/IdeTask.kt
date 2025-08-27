package org.hyperskill.academy.learning.courseFormat.tasks

import org.hyperskill.academy.learning.courseFormat.CheckStatus
import java.util.*

class IdeTask : Task {

  //used for deserialization
  constructor() : super()

  constructor(name: String) : super(name)

  constructor(name: String, id: Int, position: Int, updateDate: Date, status: CheckStatus) : super(name, id, position, updateDate, status)

  override val itemType: String = IDE_TASK_TYPE

  override val isToSubmitToRemote: Boolean
    get() = checkStatus != CheckStatus.Unchecked

  companion object {
    const val IDE_TASK_TYPE: String = "ide"
  }
}