package org.hyperskill.academy.learning.stepik

import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PYTHON_3_VERSION

/**
 * Base on a class from intellij plugin from Stepik
 *
 * @see <a href="https://github.com/StepicOrg/intellij-plugins/blob/develop/stepik-union/src/main/java/org/stepik/core/SupportedLanguages.kt"> SupportedLanguages.kt</a>
 *
 */

private const val DEFAULT_VERSION = ""

enum class StepikLanguage(val id: String?, val version: String = DEFAULT_VERSION, val langName: String?, val isDefault: Boolean = true) {
  JAVA(EduFormatNames.JAVA, "8", "java8"),
  JAVA11(EduFormatNames.JAVA, "11", "java11", isDefault = false),
  KOTLIN(EduFormatNames.KOTLIN, langName = "kotlin"),
  PYTHON(EduFormatNames.PYTHON, PYTHON_3_VERSION, langName = "python3"),
  JAVASCRIPT(EduFormatNames.JAVASCRIPT, langName = "javascript"),
  SCALA(EduFormatNames.SCALA, langName = "scala"),
  CPP(EduFormatNames.CPP, langName = "c++"),
  GO(EduFormatNames.GO, langName = "go"),
  PLAINTEXT("TEXT", langName = "TEXT"), // added for tests
  INVALID(null, langName = null);


  override fun toString(): String = id ?: ""

  companion object {
    private val TITLE_MAP: Map<Pair<String?, String>, StepikLanguage> by lazy {
      values().associateBy { it.id to it.version }
    }

    private val DEFAULT_TITLE_MAP: Map<String?, StepikLanguage> by lazy {
      values().filter { it.isDefault }.associateBy { it.id }
    }

    fun langOfId(lang: String, version: String?): StepikLanguage {
      return if (version.isNullOrEmpty()) {
        DEFAULT_TITLE_MAP.getOrElse(lang) { INVALID }
      }
      else {
        TITLE_MAP.getOrElse(lang to version) { INVALID }
      }
    }
  }
}