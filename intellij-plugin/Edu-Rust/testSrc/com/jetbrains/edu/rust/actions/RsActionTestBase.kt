package org.hyperskill.academy.rust.actions

import com.intellij.testFramework.LightPlatformTestCase
import org.hyperskill.academy.learning.EduActionTestCase
import org.intellij.lang.annotations.Language
import org.rust.cargo.CargoConstants

abstract class RsActionTestBase : EduActionTestCase() {

  protected fun checkCargoToml(@Language("TOML") expectedText: String) {
    val manifest = LightPlatformTestCase.getSourceRoot().findChild(CargoConstants.MANIFEST_FILE)
                   ?: error("Failed to find root ${CargoConstants.MANIFEST_FILE}")
    myFixture.openFileInEditor(manifest)
    myFixture.checkResult(expectedText.trimIndent())
  }
}
