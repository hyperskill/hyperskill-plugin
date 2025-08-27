package org.hyperskill.academy.learning.checker

import org.hyperskill.academy.learning.courseFormat.CheckResult

interface StderrAnalyzer {
  fun tryToGetCheckResult(stderr: String): CheckResult?
}