package org.hyperskill.academy.learning.checker

import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.Task

open class EnvironmentChecker {
  /**
   * @return null means that there are no problems with environment
   */
  @RequiresBackgroundThread
  open fun getEnvironmentError(project: Project, task: Task): CheckResult? = null
}