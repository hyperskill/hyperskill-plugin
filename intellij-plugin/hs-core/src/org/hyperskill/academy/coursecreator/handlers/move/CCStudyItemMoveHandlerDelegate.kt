package org.hyperskill.academy.coursecreator.handlers.move

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.refactoring.move.MoveHandlerDelegate
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.learning.courseFormat.StudyItem

abstract class CCStudyItemMoveHandlerDelegate(private val itemType: StudyItemType) : MoveHandlerDelegate() {

  override fun canMove(dataContext: DataContext): Boolean {
    val directory = CommonDataKeys.PSI_ELEMENT.getData(dataContext) as? PsiDirectory ?: return false
    return isAvailable(directory)
  }

  override fun canMove(elements: Array<PsiElement>, targetContainer: PsiElement?, reference: PsiReference?): Boolean {
    val directory = elements.singleOrNull() as? PsiDirectory ?: return false
    return isAvailable(directory)
  }

  override fun isValidTarget(psiElement: PsiElement?, sources: Array<PsiElement>): Boolean = true

  protected open fun getDelta(project: Project, targetItem: StudyItem): Int? {
    return showMoveStudyItemDialog(project, itemType, targetItem.name)
  }

  override fun tryToMove(
    element: PsiElement,
    project: Project,
    dataContext: DataContext,
    reference: PsiReference?,
    editor: Editor?
  ): Boolean {
    return false
  }

  protected abstract fun isAvailable(directory: PsiDirectory): Boolean
}
