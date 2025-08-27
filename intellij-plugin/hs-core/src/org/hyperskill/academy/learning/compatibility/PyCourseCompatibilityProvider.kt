package org.hyperskill.academy.learning.compatibility

import com.intellij.util.PlatformUtils.*
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos.PYTHON_COMMUNITY
import org.hyperskill.academy.learning.courseFormat.PluginInfos.TOML
import javax.swing.Icon

class PyCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    val requiredPlugins = mutableListOf<PluginInfo>()
    @Suppress("DEPRECATION", "UnstableApiUsage")
    requiredPlugins += when {
      // Actually, `isPyCharm()` covers DataSpell case as well, so `isDataSpell()` is only for readability improvement
      isPyCharm() || isDataSpell() ||
      isCLion() || isIntelliJ() || EduUtilsKt.isAndroidStudio() -> PYTHON_COMMUNITY

      else -> return null
    }
    requiredPlugins += TOML
    return requiredPlugins
  }

  override val technologyName: String get() = "Python"
  override val logo: Icon get() = EducationalCoreIcons.Language.Python
}
