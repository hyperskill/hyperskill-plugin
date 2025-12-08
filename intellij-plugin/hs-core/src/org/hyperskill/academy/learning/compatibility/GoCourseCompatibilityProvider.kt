package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import org.hyperskill.academy.learning.platform.IdeDetector
import javax.swing.Icon

class GoCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo>? =
    if (IdeDetector.isIdeaUltimate() || IdeDetector.isGoLand()) listOf(PluginInfos.GO) else null

  override val technologyName: String get() = "Go"
  override val logo: Icon get() = EducationalCoreIcons.Language.Go
}
