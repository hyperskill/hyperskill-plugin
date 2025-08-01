package org.hyperskill.academy.learning.newproject.ui.errors

sealed class SettingsValidationResult {
  object Pending : SettingsValidationResult()

  class Ready(val validationMessage: ValidationMessage?) : SettingsValidationResult()

  companion object {
    val OK: SettingsValidationResult = Ready(null)
  }
}