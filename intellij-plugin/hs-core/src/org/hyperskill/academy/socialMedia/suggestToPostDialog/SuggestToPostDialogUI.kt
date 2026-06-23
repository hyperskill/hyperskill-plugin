package org.hyperskill.academy.socialMedia.suggestToPostDialog

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.isUnitTestMode
import org.jetbrains.annotations.TestOnly

interface SuggestToPostDialogUI {
  /**
   * Shows the dialog and returns `true` if the user pressed the "Learn more" (OK) button.
   */
  fun showAndGet(): Boolean
}

fun createSuggestToPostDialogUI(project: Project, message: String): SuggestToPostDialogUI {
  return if (isUnitTestMode) {
    MOCK ?: error("You should set mock UI via `withMockSuggestToPostDialogUI`")
  }
  else {
    SuggestToPostDialog(project, message)
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
