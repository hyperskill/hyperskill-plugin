package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import org.hyperskill.academy.learning.platform.IdeDetector
import javax.swing.Icon

class PhpCourseCompatibilityProvider : CourseCompatibilityProvider {

  override val logo: Icon get() = EducationalCoreIcons.Language.Php

  override val technologyName: String get() = "PHP"

  override fun requiredPlugins(): List<PluginInfo>? {
    return if (IdeDetector.isPhpStorm() || IdeDetector.isIdeaUltimate()) listOf(PluginInfos.PHP) else null
  }
}