package org.hyperskill.academy.go.slow.checker

import org.hyperskill.academy.go.GoProjectSettings
import org.hyperskill.academy.learning.checker.CheckersTestBase
import org.hyperskill.academy.learning.checker.EduCheckerFixture

// This test runs only when GO_SDK environment variable is defined and points to the valid Go SDK.
abstract class GoCheckersTestBase : CheckersTestBase<GoProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<GoProjectSettings> = GoCheckerFixture()
}
