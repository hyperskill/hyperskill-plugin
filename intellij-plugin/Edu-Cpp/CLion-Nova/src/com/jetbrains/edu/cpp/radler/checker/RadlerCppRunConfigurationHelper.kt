package org.hyperskill.academy.cpp.radler.checker

import com.intellij.psi.PsiElement
import com.jetbrains.cidr.radler.symbols.RadMainPsiElement
import org.hyperskill.academy.cpp.checker.CppRunConfigurationHelper

class RadlerCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(entryPoint: PsiElement): PsiElement =
    RadMainPsiElement(entryPoint.containingFile, entryPoint.textRange)
}