package org.hyperskill.academy.jvm.slow.checker

import org.hamcrest.Matcher
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.learning.checker.CheckersTestBase
import org.hyperskill.academy.learning.checker.EduCheckerFixture
import org.hyperskill.academy.learning.courseFormat.CheckResultDiff
import org.hyperskill.academy.learning.courseFormat.EduTestInfo

abstract class JdkCheckerTestBase : CheckersTestBase<JdkProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<JdkProjectSettings> = JdkCheckerFixture()

  protected data class TestComparisonData(
    val messageMatcher: Matcher<String>,
    val diffMatcher: Matcher<CheckResultDiff?>,
    val executedTestsInfo: List<EduTestInfo> = emptyList()
  )
}
