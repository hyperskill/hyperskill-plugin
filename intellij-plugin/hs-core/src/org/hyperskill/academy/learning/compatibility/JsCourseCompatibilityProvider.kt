package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import org.hyperskill.academy.learning.platform.IdeDetector
import javax.swing.Icon

class JsCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    val supportsJavaScript = IdeDetector.isIntelliJ() ||
                             IdeDetector.isWebStorm() ||
                             IdeDetector.isPyCharm() ||
                             IdeDetector.isGoLand()
    return if (supportsJavaScript) {
      listOf(
        PluginInfos.JAVA_SCRIPT,
        PluginInfos.JAVA_SCRIPT_DEBUGGER,
        PluginInfos.NODE_JS
      )
    }
    else {
      null
    }
  }

  override val technologyName: String get() = "JavaScript"
  override val logo: Icon get() = EducationalCoreIcons.Language.JavaScript
}