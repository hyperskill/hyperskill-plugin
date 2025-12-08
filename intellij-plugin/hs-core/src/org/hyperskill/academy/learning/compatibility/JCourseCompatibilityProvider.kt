package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import org.hyperskill.academy.learning.platform.IdeDetector
import javax.swing.Icon

class JCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    if (!IdeDetector.isIntelliJ()) return null
    return listOf(
      PluginInfos.JAVA,
      PluginInfos.GRADLE,
      PluginInfos.JUNIT
    )
  }

  override val technologyName: String get() = "Java"
  override val logo: Icon get() = EducationalCoreIcons.Language.Java
}
