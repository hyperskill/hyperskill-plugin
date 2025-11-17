package org.hyperskill.academy.ai.debugger.java.impl

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import org.hyperskill.academy.ai.debugger.core.api.TestFinder

class JTestFinder : TestFinder {
  override fun findTestByName(project: Project, testFiles: List<VirtualFile>, testName: String): String? {
    // Returns the entire test class because Java tests don't have separate methods for each test
    val className = testName.substringBefore(":")
    val psiManager = PsiManager.getInstance(project)
    return testFiles.asSequence()
      .mapNotNull { psiManager.findFile(it) }
      .flatMap { PsiTreeUtil.findChildrenOfType(it, PsiClass::class.java) }
      .firstOrNull { it.qualifiedName == className }
      ?.text
  }
}