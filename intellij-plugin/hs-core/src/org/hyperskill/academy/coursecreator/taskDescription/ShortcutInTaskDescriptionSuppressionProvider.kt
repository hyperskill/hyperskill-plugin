package org.hyperskill.academy.coursecreator.taskDescription

import com.intellij.codeInspection.DefaultXmlSuppressionProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.hyperskill.academy.learning.taskToolWindow.containsShortcut

class ShortcutInTaskDescriptionSuppressionProvider : DefaultXmlSuppressionProvider() {

  override fun isSuppressedFor(element: PsiElement, inspectionId: String): Boolean =
    inspectionId == "CheckDtdRefs" && element.text.containsShortcut()

  override fun isProviderAvailable(file: PsiFile): Boolean = false
}