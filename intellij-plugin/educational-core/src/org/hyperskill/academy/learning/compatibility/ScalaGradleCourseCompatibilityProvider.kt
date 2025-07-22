package org.hyperskill.academy.learning.compatibility

import com.intellij.util.PlatformUtils
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

class ScalaGradleCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!PlatformUtils.isIntelliJ()) return null
    return listOf(
      PluginInfos.SCALA,
      PluginInfos.JAVA,
      PluginInfos.GRADLE,
      PluginInfos.JUNIT
    )
  }

  override val technologyName: String get() = "Scala"
  override val logo: Icon get() = EducationalCoreIcons.Language.Scala
}
