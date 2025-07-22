package org.hyperskill.academy.rust.slow.checker

import org.hyperskill.academy.learning.checker.EduCheckerFixture
import org.hyperskill.academy.rust.RsProjectSettings
import org.rust.cargo.toolchain.RsToolchainBase

class RsCheckerFixture : EduCheckerFixture<RsProjectSettings>() {

  private var toolchain: RsToolchainBase? = null

  override val projectSettings: RsProjectSettings get() = RsProjectSettings(toolchain)

  override fun getSkipTestReason(): String? = if (toolchain == null) "no Rust toolchain found" else super.getSkipTestReason()

  override fun setUp() {
    super.setUp()
    toolchain = RsToolchainBase.suggest()
  }
}
