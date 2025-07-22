package org.hyperskill.academy.python.learning.highlighting

import com.intellij.psi.PsiReference
import com.intellij.psi.util.parentOfType
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyImportStatementBase
import com.jetbrains.python.psi.types.TypeEvalContext
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.EduFileErrorHighlightLevel
import org.hyperskill.academy.learning.getTaskFile

class PyEduInspectionExtension : PyInspectionExtension() {
  override fun ignoreUnresolvedReference(element: PyElement, reference: PsiReference, context: TypeEvalContext): Boolean {
    val file = element.containingFile ?: return false
    val project = file.project
    if (project.course == null) {
      return false
    }
    val taskFile = file.virtualFile?.getTaskFile(project)
    if (taskFile == null || taskFile.errorHighlightLevel != EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION) {
      return false
    }
    return element.parentOfType<PyImportStatementBase>() == null
  }
}
