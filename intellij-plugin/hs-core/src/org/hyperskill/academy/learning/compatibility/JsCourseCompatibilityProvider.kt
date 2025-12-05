package org.hyperskill.academy.learning.compatibility

import com.intellij.util.PlatformUtils
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

class JsCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? {
    val prefix = PlatformUtils.getPlatformPrefix()
    val supportsJavaScript = prefix == PlatformUtils.IDEA_PREFIX ||
                             prefix == PlatformUtils.WEB_PREFIX ||
                             prefix == PlatformUtils.PYCHARM_PREFIX ||
                             prefix == PlatformUtils.GOIDE_PREFIX
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