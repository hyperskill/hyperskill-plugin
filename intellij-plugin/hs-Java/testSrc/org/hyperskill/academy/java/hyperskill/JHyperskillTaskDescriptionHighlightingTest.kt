package org.hyperskill.academy.java.hyperskill

import com.intellij.lang.Language
import com.intellij.lang.java.JavaLanguage
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillTaskDescriptionHighlightingTest

class JHyperskillTaskDescriptionHighlightingTest : HyperskillTaskDescriptionHighlightingTest() {
  override val codeSample: String get() = """class Main {}"""

  override val codeSampleWithHighlighting: String
    get() = """<span style="...">class </span><span style="...">Main {}</span>"""

  override val language: Language get() = JavaLanguage.INSTANCE

}
