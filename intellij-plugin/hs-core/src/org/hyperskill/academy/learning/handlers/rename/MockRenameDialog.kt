package org.hyperskill.academy.learning.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement

class MockRenameDialogFactory(
  private val newItemName: String
) : RenameDialogFactory {

  var delegate: RenameDialogFactory? = null

  var isDialogShown: Boolean = false
  var isRenameInvoked: Boolean = false

  private val dialogs = mutableListOf<EduRenameDialogBase>()

  override fun createRenameDialog(
    project: Project,
    element: PsiElement,
    nameSuggestionContext: PsiElement?,
    editor: Editor?
  ): EduRenameDialogBase {
    val delegateDialog = delegate!!.createRenameDialog(project, element, nameSuggestionContext, editor)
    val mockDialog = MockRenameDialog(this, delegateDialog, project, element, nameSuggestionContext, editor)
    mockDialog.setMockNewName(newItemName)
    dialogs += delegateDialog
    dialogs += mockDialog
    return mockDialog
  }

  /**
   * The dialogs are never shown in tests, so nothing disposes them otherwise.
   * Their scope choosers keep computing search scopes in the background after the test,
   * against a project that is already reused by another test
   */
  fun disposeDialogs() {
    dialogs.forEach { it.disposeIfNeeded() }
    dialogs.clear()
  }
}

class MockRenameDialog(
  private val factory: MockRenameDialogFactory,
  private val delegate: EduRenameDialogBase,
  project: Project,
  element: PsiElement,
  nameSuggestionContext: PsiElement?,
  editor: Editor?
) : EduRenameDialogBase(project, element, nameSuggestionContext, editor) {

  override fun setMockNewName(newName: String) {
    super.setMockNewName(newName)
    delegate.setMockNewName(newName)
  }

  override fun performRename(newName: String) {
    factory.isDialogShown = true
    try {
      // Unfortunately, rename handler doesn't check if rename action is available in tests
      // so let's do it ourselves
      delegate.canRun()
      factory.isRenameInvoked = true
      delegate.performRename(newName)
    }
    catch (e: ConfigurationException) {
    }
  }
}
