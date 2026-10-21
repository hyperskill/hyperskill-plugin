package org.hyperskill.academy.cpp

import com.intellij.clion.radler.core.settings.migration.OCLanguageStub
import com.intellij.lang.Language

/**
 * The language C/C++ courses are created with in tests (its id is `ObjectiveC`, see `EduFormatNames.CPP`).
 *
 * Since 262 CLion has only the Nova engine, which keeps the `ObjectiveC` language only as [OCLanguageStub]
 */
internal val cppLanguage: Language
  get() = OCLanguageStub
