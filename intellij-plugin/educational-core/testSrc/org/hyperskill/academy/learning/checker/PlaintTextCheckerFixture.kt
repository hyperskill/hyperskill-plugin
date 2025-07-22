package org.hyperskill.academy.learning.checker

import org.hyperskill.academy.learning.newproject.EmptyProjectSettings

class PlaintTextCheckerFixture : EduCheckerFixture<EmptyProjectSettings>() {
  override val projectSettings: EmptyProjectSettings get() = EmptyProjectSettings
}
