package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.SHELL
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

class ShellCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo> = listOf(PluginInfos.SHELL)

  override val technologyName: String get() = SHELL
  override val logo: Icon get() = EducationalCoreIcons.Language.Shell
}