package org.hyperskill.academy.learning.newproject.ui.errors

sealed class SettingsValidationResult {
  object Pending : SettingsValidationResult()

  class Ready(val validationMessage: ValidationMessage?) : SettingsValidationResult()

  /**
   * Non-blocking counterpart of [Ready]: the message is shown to the user, but the course can still be started.
   *
   * Use it when the settings are not perfect yet, and the plugin is able to fix them on its own while the project
   * is being created, e.g. by downloading the JDK the course requires.
   */
  class ReadyWithWarning(val validationMessage: ValidationMessage) : SettingsValidationResult()

  companion object {
    val OK: SettingsValidationResult = Ready(null)
  }
}
