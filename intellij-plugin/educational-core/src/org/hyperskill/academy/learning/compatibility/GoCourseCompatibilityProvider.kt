package org.hyperskill.academy.learning.compatibility

import com.intellij.util.PlatformUtils.isGoIde
import com.intellij.util.PlatformUtils.isIdeaUltimate
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

class GoCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? =
    if (isIdeaUltimate() || isGoIde()) listOf(PluginInfos.GO) else null

  override val technologyName: String get() = "Go"
  override val logo: Icon get() = EducationalCoreIcons.Language.Go
}
