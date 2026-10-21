package org.hyperskill.academy.cpp

import com.intellij.lang.Language
import com.jetbrains.cidr.lang.OCLanguage

/**
 * The language C/C++ courses are created with in tests (its id is `ObjectiveC`, see `EduFormatNames.CPP`).
 *
 * BACKCOMPAT: 261 -- the classic engine providing `OCLanguage` was removed from CLion in 262
 */
internal val cppLanguage: Language
  get() = OCLanguage.getInstance()
