package org.hyperskill.academy.go.hyperskill

import com.goide.GoLanguage
import com.intellij.lang.Language
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillTaskDescriptionHighlightingTest

class GoHyperskillTaskDescriptionHighlightingTest : HyperskillTaskDescriptionHighlightingTest() {
  override val language: Language
    get() = GoLanguage.INSTANCE

  override val codeSample: String
    get() = """fun main() {}"""

  override val codeSampleWithHighlighting: String
    get() = """<span style="...">fun main</span><span style="...">() {}</span>"""
}
