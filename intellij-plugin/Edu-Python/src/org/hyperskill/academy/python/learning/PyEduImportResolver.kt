package org.hyperskill.academy.python.learning

import com.intellij.psi.PsiElement
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.impl.PyImportResolver
import com.jetbrains.python.psi.resolve.PyQualifiedNameResolveContext
import org.hyperskill.academy.learning.course

class PyEduImportResolver : PyImportResolver {
  override fun resolveImportReference(
    name: QualifiedName,
    context: PyQualifiedNameResolveContext,
    withRoots: Boolean
  ): PsiElement? {
    if (context.project.course == null) {
      return null
    }
    val containingFile = context.footholdFile ?: return null
    val directory = containingFile.containingDirectory ?: return null
    return directory.findFile("$name.py")
  }
}
