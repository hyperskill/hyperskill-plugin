package org.hyperskill.academy.cpp.radler.checker

import com.intellij.psi.PsiElement
import org.hyperskill.academy.cpp.checker.CppRunConfigurationHelper

// In 253, the radler package was removed and the API changed significantly.
// TODO: BACKCOMPAT 253 - Find the new API to wrap entry points for run configuration
class RadlerCppRunConfigurationHelper : CppRunConfigurationHelper {
  override fun prepareEntryPointForRunConfiguration(entryPoint: PsiElement): PsiElement {
    // In CLion Nova 253, RadMainPsiElement from radler.symbols was removed.
    // Return the entryPoint directly for now. If run configuration creation fails,
    // the implementation needs to be updated to use the new API.
    return entryPoint
  }
}
