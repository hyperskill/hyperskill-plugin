package org.hyperskill.academy.python.learning.checker

import org.hyperskill.academy.learning.checker.DefaultCodeExecutor
import org.hyperskill.academy.learning.courseFormat.CheckResult

class PyCodeExecutor : DefaultCodeExecutor() {

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? = PyStderrAnalyzer.tryToGetCheckResult(errorOutput)
}
