package org.hyperskill.academy.python.learning.stepik.hyperskill

import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.EduUtilsKt.isAndroidStudio
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator
import org.hyperskill.academy.python.learning.PyConfigurator.Companion.MAIN_PY
import org.hyperskill.academy.python.learning.PyNewConfigurator
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings

class PyUnittestHyperskillConfigurator : HyperskillConfigurator<PyProjectSettings>(PyNewConfigurator()) {
  override val testDirs: List<String> = listOf(EduNames.TEST)
  override val isEnabled: Boolean = !isAndroidStudio()

  override fun getMockFileName(course: Course, text: String): String = MAIN_PY
}