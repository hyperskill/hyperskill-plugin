package org.hyperskill.academy.javascript.learning.stepik.hyperskill

import org.hyperskill.academy.javascript.learning.JsConfigurator
import org.hyperskill.academy.javascript.learning.JsConfigurator.Companion.MAIN_JS
import org.hyperskill.academy.javascript.learning.JsNewProjectSettings
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator

class JsHyperskillConfigurator : HyperskillConfigurator<JsNewProjectSettings>(JsConfigurator()) {
  override val testDirs: List<String>
    get() = listOf(HYPERSKILL_TEST_DIR, EduNames.TEST)

  override fun getMockFileName(course: Course, text: String): String = MAIN_JS
}
