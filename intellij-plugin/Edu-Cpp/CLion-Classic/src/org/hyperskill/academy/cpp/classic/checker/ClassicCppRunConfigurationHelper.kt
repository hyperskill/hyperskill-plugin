package org.hyperskill.academy.cpp.classic.checker

import com.intellij.psi.PsiElement
import org.hyperskill.academy.cpp.checker.CppRunConfigurationHelper

class ClassicCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(entryPoint: PsiElement): PsiElement = entryPoint
}