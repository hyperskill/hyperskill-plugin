package org.hyperskill.academy.shell

import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings

class ShellLanguageSettings : LanguageSettings<EmptyProjectSettings>() {
  override fun getSettings(): EmptyProjectSettings = EmptyProjectSettings
}