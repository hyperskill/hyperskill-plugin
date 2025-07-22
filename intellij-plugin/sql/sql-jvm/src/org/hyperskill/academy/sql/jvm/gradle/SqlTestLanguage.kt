package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.lang.Language
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import javax.swing.Icon

const val SQL_TEST_LANGUAGE_KEY = "sql_test_language"

var Course.sqlTestLanguage: SqlTestLanguage
  get() {
    val languageId = environmentSettings[SQL_TEST_LANGUAGE_KEY]
    return SqlTestLanguage.values().find { it.languageId == languageId } ?: SqlTestLanguage.KOTLIN
  }
  set(value) {
    environmentSettings += SQL_TEST_LANGUAGE_KEY to value.languageId
  }

/**
 * Default programming language used for test files
 */
enum class SqlTestLanguage(val languageId: String, val logo: Icon) {
  KOTLIN(EduFormatNames.KOTLIN, EducationalCoreIcons.Language.Kotlin),
  JAVA(EduFormatNames.JAVA, EducationalCoreIcons.Language.Java);

  fun getLanguage(): Language? = Language.findLanguageByID(languageId)
}

