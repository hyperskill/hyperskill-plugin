package org.hyperskill.academy.learning.handlers

import com.intellij.codeInsight.hint.HintManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.ReadOnlyFragmentModificationException
import com.intellij.openapi.editor.actionSystem.ReadonlyFragmentModificationHandler
import org.hyperskill.academy.learning.messages.EduCoreBundle.message

class AnswerPlaceholderDeleteHandler(private val editor: Editor) : ReadonlyFragmentModificationHandler {
  override fun handle(e: ReadOnlyFragmentModificationException) {
    if (editor.isDisposed) return
    HintManager.getInstance().showErrorHint(editor, message("notification.text.error.hint.placeholder.delete"))
  }
}
