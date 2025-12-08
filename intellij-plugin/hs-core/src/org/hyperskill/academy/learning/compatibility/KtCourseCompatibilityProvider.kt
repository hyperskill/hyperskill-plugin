package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import org.hyperskill.academy.learning.platform.IdeDetector
import javax.swing.Icon

class KtCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    if (!IdeDetector.isIntelliJ() && !IdeDetector.isAndroidStudio()) return null
    return listOf(
      PluginInfos.KOTLIN,
      PluginInfos.JAVA,
      PluginInfos.GRADLE,
      PluginInfos.JUNIT
    )
  }

  override val technologyName: String get() = "Kotlin"
  override val logo: Icon get() = EducationalCoreIcons.Language.Kotlin
}
