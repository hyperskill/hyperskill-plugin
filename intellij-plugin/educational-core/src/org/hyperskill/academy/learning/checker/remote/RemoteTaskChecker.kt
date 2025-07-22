package org.hyperskill.academy.learning.checker.remote

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.Task

interface RemoteTaskChecker {

  fun canCheck(project: Project, task: Task): Boolean

  fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult

  fun retry(task: Task): Result<Boolean, String>? {
    return null
  }
}
