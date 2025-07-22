package org.hyperskill.academy.javascript.hyperskill

import com.intellij.lang.Language
import com.intellij.lang.javascript.JavascriptLanguage
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillTaskDescriptionHighlightingTest

class JsHyperskillTaskDescriptionHighlightingTest : HyperskillTaskDescriptionHighlightingTest() {
  override val language: Language
    get() = JavascriptLanguage

  override val codeSample: String
    get() = """console.log(1)"""

  override val codeSampleWithHighlighting: String
    get() = """<span style="...">console.log(</span><span style="...">1</span><span style="...">)</span>"""
}
