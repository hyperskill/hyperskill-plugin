package org.hyperskill.academy.python.hyperskill

import com.intellij.lang.Language
import com.intellij.util.ThrowableRunnable
import com.jetbrains.python.PythonLanguage
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillTaskDescriptionHighlightingTest

class PyHyperskillTaskDescriptionHighlightingTest : HyperskillTaskDescriptionHighlightingTest() {

  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    // Hyperskill python support is not available in Android Studio
    if (!EduUtilsKt.isAndroidStudio()) {
      super.runTestRunnable(context)
    }
  }

  override val language: Language
    get() = PythonLanguage.INSTANCE

  override val codeSample: String
    get() = """print(1)"""
  override val codeSampleWithHighlighting: String
    get() = """<span style="...">print(</span><span style="...">1</span><span style="...">)</span>"""
}
