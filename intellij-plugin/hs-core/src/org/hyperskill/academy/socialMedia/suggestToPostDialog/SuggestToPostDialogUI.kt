package org.hyperskill.academy.socialMedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

interface SuggestToPostDialogUI {
  /**
   * Shows the dialog. Returns `true` only if it was closed via an OK action.
   * Sharing happens through in-dialog actions, so callers don't rely on the return value.
   */
  fun showAndGet(): Boolean
}

fun createSuggestToPostDialogUI(
  project: Project,
  message: String,
  xShareUrl: String,
  linkedInShareUrl: String
): SuggestToPostDialogUI {
  return if (isUnitTestMode) {
    MOCK ?: error("You should set mock UI via `withMockSuggestToPostDialogUI`")
  }
  else {
    SuggestToPostDialog(project, message, xShareUrl, linkedInShareUrl)
  }
}

private var MOCK: SuggestToPostDialogUI? = null

@TestOnly
fun withMockSuggestToPostDialogUI(mockUI: SuggestToPostDialogUI, action: () -> Unit) {
  try {
    MOCK = mockUI
    action()
  }
  finally {
    MOCK = null
  }
}
