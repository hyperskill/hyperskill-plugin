package org.hyperskill.academy.learning.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.IdeTask

class IdeTaskChecker(task: IdeTask, project: Project) : TaskChecker<IdeTask>(task, project) {
  override fun check(indicator: ProgressIndicator): CheckResult {
    return CheckResult.SOLVED
  }
}