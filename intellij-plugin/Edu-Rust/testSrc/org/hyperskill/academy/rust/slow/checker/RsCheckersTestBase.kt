package org.hyperskill.academy.rust.slow.checker

import org.hyperskill.academy.learning.checker.CheckersTestBase
import org.hyperskill.academy.learning.checker.EduCheckerFixture
import org.hyperskill.academy.rust.RsProjectSettings

// This test runs only when Rust toolchain is found.
abstract class RsCheckersTestBase : CheckersTestBase<RsProjectSettings>() {

  override fun createCheckerFixture(): EduCheckerFixture<RsProjectSettings> = RsCheckerFixture()
}
