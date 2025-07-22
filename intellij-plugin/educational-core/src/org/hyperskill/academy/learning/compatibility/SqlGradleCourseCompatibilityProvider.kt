package org.hyperskill.academy.learning.compatibility

import com.intellij.util.PlatformUtils
import org.hyperskill.academy.learning.EduExperimentalFeatures
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import org.hyperskill.academy.learning.isFeatureEnabled

class SqlGradleCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!isFeatureEnabled(EduExperimentalFeatures.SQL_COURSES)) return null
    @Suppress("UnstableApiUsage")
    // We need SQL plugin available only in paid IDEs and Java + Gradle plugins available in IntelliJ IDEA and Android Studio.
    // So it's only IDEA Ultimate
    if (!PlatformUtils.isIdeaUltimate()) return null
    return listOf(
      PluginInfos.SQL,
      PluginInfos.JAVA,
      PluginInfos.GRADLE,
      PluginInfos.JUNIT
    )
  }

  override val technologyName: String = "SQL"
}
