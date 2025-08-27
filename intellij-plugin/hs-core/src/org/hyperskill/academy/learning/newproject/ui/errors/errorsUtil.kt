package org.hyperskill.academy.learning.newproject.ui.errors

import org.hyperskill.academy.learning.EduBrowser
import org.hyperskill.academy.learning.courseFormat.Course

fun getErrorState(course: Course?, validateSettings: (Course) -> SettingsValidationResult): ErrorState {
  var languageError: ErrorState = ErrorState.NothingSelected
  if (course != null) {
    val validationResult = validateSettings(course)

    languageError = when (validationResult) {
      is SettingsValidationResult.Pending -> ErrorState.Pending
      is SettingsValidationResult.Ready -> {
        val validationMessage = validationResult.validationMessage
        validationMessage?.let { ErrorState.LanguageSettingsError(it) } ?: ErrorState.None
      }
    }
  }
  return ErrorState.forCourse(course).merge(languageError)
}

fun browseHyperlink(message: ValidationMessage?) {
  if (message == null) {
    return
  }
  val hyperlink = message.hyperlinkAddress
  if (hyperlink != null) {
    EduBrowser.getInstance().browse(hyperlink)
  }
}