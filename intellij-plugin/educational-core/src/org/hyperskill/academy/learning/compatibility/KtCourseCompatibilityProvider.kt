package org.hyperskill.academy.learning.compatibility

import com.intellij.util.PlatformUtils
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

class KtCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    if (!PlatformUtils.isIntelliJ() && !EduUtilsKt.isAndroidStudio()) return null
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
