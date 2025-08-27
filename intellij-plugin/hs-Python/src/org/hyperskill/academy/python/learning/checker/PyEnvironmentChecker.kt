package org.hyperskill.academy.python.learning.checker

import com.intellij.openapi.project.Project
import com.jetbrains.python.sdk.pythonSdk
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.python.learning.messages.EduPythonBundle

class PyEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    return if (project.pythonSdk == null) {
      CheckResult(CheckStatus.Unchecked, EduPythonBundle.message("error.no.python.interpreter", ENVIRONMENT_CONFIGURATION_LINK_PYTHON))
    }
    else null
  }
}