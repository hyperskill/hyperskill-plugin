package org.hyperskill.academy.learning

import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task

data class FileCheck(
  val task: Task,
  val path: String,
  val shouldContain: Boolean,
  val additionalCheck: ((TaskFile) -> Unit)? = null
) {
  fun check() {
    val taskFile = task.taskFiles[path]
    if (shouldContain) {
      check(taskFile != null) {
        "`$path` should be in `${task.name}` task"
      }
      additionalCheck?.invoke(taskFile) // !! is safe because of `check` call
    }
    else {
      check(taskFile == null) {
        "`$path` shouldn't be in `${task.name}` task"
      }
    }
  }

  fun withAdditionalCheck(check: (TaskFile) -> Unit): FileCheck = copy(additionalCheck = check)
}

infix fun String.`in`(task: Task): FileCheck = FileCheck(task, this, true)
