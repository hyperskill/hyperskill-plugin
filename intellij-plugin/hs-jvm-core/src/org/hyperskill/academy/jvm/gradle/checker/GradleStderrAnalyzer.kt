package org.hyperskill.academy.jvm.gradle.checker

import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.checker.StderrAnalyzer
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus

object GradleStderrAnalyzer : StderrAnalyzer {
  override fun tryToGetCheckResult(stderr: String): CheckResult? {
    for (error in CheckUtils.COMPILATION_ERRORS) {
      if (error in stderr) return CheckResult(CheckStatus.Failed, CheckUtils.COMPILATION_FAILED_MESSAGE, stderr)
    }
    return null
  }
}