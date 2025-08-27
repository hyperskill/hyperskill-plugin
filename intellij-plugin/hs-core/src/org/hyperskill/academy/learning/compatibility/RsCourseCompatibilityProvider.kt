package org.hyperskill.academy.learning.compatibility

import com.intellij.util.PlatformUtils.*
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

private val RUST_PLUGINS = listOf(
  PluginInfos.RUST,
  PluginInfos.TOML
)

class RsCourseCompatibilityProvider : CourseCompatibilityProvider {
  @Suppress("UnstableApiUsage", "DEPRECATION")
  override fun requiredPlugins(): List<PluginInfo>? = if (isIdeaUltimate() || isCLion() || isRustRover()) RUST_PLUGINS else null

  override val technologyName: String get() = "Rust"
  override val logo: Icon get() = EducationalCoreIcons.Language.Rust
}
