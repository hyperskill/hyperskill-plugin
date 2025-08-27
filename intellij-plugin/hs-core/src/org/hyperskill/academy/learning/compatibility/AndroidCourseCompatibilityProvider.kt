package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

class AndroidCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    if (!EduUtilsKt.isAndroidStudio()) return null
    return listOf(
      PluginInfos.ANDROID,
      PluginInfos.KOTLIN,
      PluginInfos.JAVA,
      PluginInfos.GRADLE,
      PluginInfos.JUNIT
    )
  }

  override val technologyName: String get() = "Android"
  override val logo: Icon get() = EducationalCoreIcons.Language.Android
}
