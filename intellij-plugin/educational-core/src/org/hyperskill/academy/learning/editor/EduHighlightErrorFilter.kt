package org.hyperskill.academy.learning.editor

import com.intellij.codeInsight.highlighting.HighlightErrorFilter
import com.intellij.psi.PsiErrorElement
import org.hyperskill.academy.learning.courseFormat.EduFileErrorHighlightLevel
import org.hyperskill.academy.learning.getTaskFile

class EduHighlightErrorFilter : HighlightErrorFilter() {
  override fun shouldHighlightErrorElement(element: PsiErrorElement): Boolean {
    val file = element.containingFile ?: return true
    val virtualFile = file.virtualFile ?: return true
    val taskFile = virtualFile.getTaskFile(element.project)
    return taskFile == null || taskFile.errorHighlightLevel !== EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
  }
}
