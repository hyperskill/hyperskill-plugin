package org.hyperskill.academy.javascript.learning.checker

import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.checker.DefaultCodeExecutor
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus


class JsCodeExecutor : DefaultCodeExecutor() {

  private val syntaxError = listOf("SyntaxError")

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? {
    return when {
      syntaxError.any { it in errorOutput } -> CheckResult(CheckStatus.Failed, CheckUtils.SYNTAX_ERROR_MESSAGE, errorOutput)
      else -> null
    }
  }
}
