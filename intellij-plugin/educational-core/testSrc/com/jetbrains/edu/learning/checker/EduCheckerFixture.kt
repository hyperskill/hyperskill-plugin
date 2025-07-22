package org.hyperskill.academy.learning.checker

import com.intellij.testFramework.fixtures.impl.BaseFixture
import org.hyperskill.academy.learning.newproject.EduProjectSettings

abstract class EduCheckerFixture<Settings : EduProjectSettings> : BaseFixture() {
  abstract val projectSettings: Settings
  open fun getSkipTestReason(): String? = null
}
