package org.hyperskill.academy.learning.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import org.hyperskill.academy.learning.handlers.shouldEduTaskFileRenameProcessorHandle
import org.hyperskill.academy.learning.messages.EduCoreBundle

class EduTaskFileRenameProcessor : RenamePsiFileProcessor() {

  override fun canProcessElement(element: PsiElement): Boolean {
    if (element !is PsiFile) return false
    return shouldEduTaskFileRenameProcessorHandle(element.project, element)
  }

  override fun createRenameDialog(
    project: Project,
    element: PsiElement,
    nameSuggestionContext: PsiElement?,
    editor: Editor?
  ): RenameDialog {
    return createRenameDialog(project, element, nameSuggestionContext, editor, Factory())
  }

  private class Factory : RenameDialogFactory {

    override fun createRenameDialog(
      project: Project,
      element: PsiElement,
      nameSuggestionContext: PsiElement?,
      editor: Editor?
    ): EduRenameDialogBase {
      return object : EduRenameDialogBase(project, element, nameSuggestionContext, editor) {
        override fun canRun() {
          throw ConfigurationException(EduCoreBundle.message("error.invalid.rename.message"))
        }
      }
    }

  }
}
