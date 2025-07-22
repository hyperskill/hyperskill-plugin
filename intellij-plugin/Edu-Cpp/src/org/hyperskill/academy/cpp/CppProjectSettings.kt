package org.hyperskill.academy.cpp

import com.jetbrains.cmake.completion.CMakeRecognizedCPPLanguageStandard
import org.hyperskill.academy.learning.newproject.EduProjectSettings

data class CppProjectSettings(val languageStandard: String = CMakeRecognizedCPPLanguageStandard.CPP14.standard) : EduProjectSettings