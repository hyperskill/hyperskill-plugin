package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos.PYTHON_COMMUNITY
import org.hyperskill.academy.learning.courseFormat.PluginInfos.TOML
import org.hyperskill.academy.learning.platform.IdeDetector
import javax.swing.Icon

class PyCourseCompatibilityProvider : CourseCompatibilityProvider {

  override fun requiredPlugins(): List<PluginInfo>? {
    val requiredPlugins = mutableListOf<PluginInfo>()
    requiredPlugins += when {
      // isPyCharm() covers DataSpell case as well, so isDataSpell() is only for readability improvement
      IdeDetector.isPyCharm() || IdeDetector.isDataSpell() ||
      IdeDetector.isCLion() || IdeDetector.isIntelliJ() || IdeDetector.isAndroidStudio() -> PYTHON_COMMUNITY

      else -> return null
    }
    requiredPlugins += TOML
    return requiredPlugins
  }

  override val technologyName: String get() = "Python"
  override val logo: Icon get() = EducationalCoreIcons.Language.Python
}
