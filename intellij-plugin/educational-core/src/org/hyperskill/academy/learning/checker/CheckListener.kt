package org.hyperskill.academy.learning.checker

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.Task

interface CheckListener {
  fun beforeCheck(project: Project, task: Task) {}
  fun afterCheck(project: Project, task: Task, result: CheckResult) {}

  companion object {
    val EP_NAME: ExtensionPointName<CheckListener> = ExtensionPointName.create("HyperskillEducational.checkListener")
  }
}
