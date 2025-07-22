package org.hyperskill.academy.learning.checker

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle

/**
 * If you add any new public methods here, please do not forget to add it also to
 * @see org.hyperskill.academy.learning.stepik.hyperskill.checker.HyperskillTaskCheckerProvider
 */
open class TaskChecker<out T : Task>(val task: T, val project: Project) {
  open fun onTaskSolved() {
  }

  open fun onTaskFailed() {
  }

  open fun check(indicator: ProgressIndicator) =
    CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("check.result.for.task.not.available", task.itemType))

  open fun clearState() {}

  companion object {
    const val EP_NAME = "HyperskillEducational.taskChecker"

    val LOG = Logger.getInstance(TaskChecker::class.java)
  }
}