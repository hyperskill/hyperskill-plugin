package org.hyperskill.academy.python.learning.stepik.hyperskill

import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.EduUtilsKt.isAndroidStudio
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings

class PyHyperskillConfigurator : HyperskillConfigurator<PyProjectSettings>(PyHyperskillBaseConfigurator()) {
  override val testDirs: List<String> = listOf(HYPERSKILL_TEST_DIR, EduNames.TEST)
  override val isEnabled: Boolean = !isAndroidStudio()
  override val isCourseCreatorEnabled: Boolean = true
}
