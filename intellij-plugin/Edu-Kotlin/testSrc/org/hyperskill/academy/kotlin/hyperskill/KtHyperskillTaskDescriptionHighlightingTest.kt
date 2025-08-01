package org.hyperskill.academy.kotlin.hyperskill

import com.intellij.lang.Language
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillTaskDescriptionHighlightingTest
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtHyperskillTaskDescriptionHighlightingTest : HyperskillTaskDescriptionHighlightingTest() {
  override val language: Language
    get() = KotlinLanguage.INSTANCE

  override val codeSample: String
    get() = """fun main() {}"""
  override val codeSampleWithHighlighting: String
    get() = """<span style="...">fun </span><span style="...">main() {}</span>"""
}
