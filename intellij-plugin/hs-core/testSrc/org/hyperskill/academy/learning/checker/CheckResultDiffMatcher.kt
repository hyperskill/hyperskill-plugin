package org.hyperskill.academy.learning.checker

import org.hamcrest.BaseMatcher
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hyperskill.academy.learning.courseFormat.CheckResultDiff

class CheckResultDiffMatcher(private val expected: CheckResultDiff) : BaseMatcher<CheckResultDiff?>() {
  override fun describeTo(description: Description) {
    description.appendValue(expected)
  }

  override fun matches(actual: Any?): Boolean {
    if (actual !is CheckResultDiff) return false
    return expected.title == actual.title && expected.actual == actual.actual && expected.expected == actual.expected
  }

  companion object {
    fun diff(expected: CheckResultDiff): Matcher<CheckResultDiff?> = CheckResultDiffMatcher(expected)
  }
}
