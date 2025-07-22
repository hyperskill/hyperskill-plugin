package org.hyperskill.academy.rust.checker

import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.checker.StderrAnalyzer
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus

object RsStderrAnalyzer : StderrAnalyzer {
  override fun tryToGetCheckResult(stderr: String): CheckResult? = if (stderr.contains(COMPILATION_ERROR_MESSAGE, true)) {
    CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, stderr)
  }
  else {
    null
  }
}