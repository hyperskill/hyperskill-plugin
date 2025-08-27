package org.hyperskill.academy.learning.compatibility

import org.hyperskill.academy.learning.courseFormat.PluginInfo

sealed class CourseCompatibility {
  data object Compatible : CourseCompatibility()
  data object IncompatibleVersion : CourseCompatibility()
  class PluginsRequired(val toInstallOrEnable: List<PluginInfo>) : CourseCompatibility()
  data object Unsupported : CourseCompatibility()
}
