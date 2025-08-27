package org.hyperskill.academy.javascript.learning.checker

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.javascript.learning.NodeJS
import org.hyperskill.academy.javascript.learning.messages.EduJavaScriptBundle
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_JS
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task

class JsEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
    return if (interpreter == null || interpreter.validate(project) != null) {
      return CheckResult(
        CheckStatus.Unchecked,
        EduJavaScriptBundle.message("error.no.interpreter", NodeJS, ENVIRONMENT_CONFIGURATION_LINK_JS)
      )
    }
    else null
  }
}
