package org.hyperskill.academy.learning.compatibility

import com.intellij.util.PlatformUtils.isIdeaUltimate
import com.intellij.util.PlatformUtils.isPhpStorm
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

class PhpCourseCompatibilityProvider : CourseCompatibilityProvider {

  override val logo: Icon get() = EducationalCoreIcons.Language.Php

  override val technologyName: String get() = "PHP"

  @Suppress("UnstableApiUsage")
  override fun requiredPlugins(): List<PluginInfo>? {
    return if (isPhpStorm() || isIdeaUltimate()) listOf(PluginInfos.PHP) else null
  }
}