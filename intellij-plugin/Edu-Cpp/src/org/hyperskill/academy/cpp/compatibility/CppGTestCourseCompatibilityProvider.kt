package org.hyperskill.academy.cpp.compatibility

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.compatibility.CourseCompatibilityProvider
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import javax.swing.Icon

class CppGTestCourseCompatibilityProvider : CourseCompatibilityProvider {
  override fun requiredPlugins(): List<PluginInfo> = listOf(PluginInfos.GOOGLE_TEST)

  override val technologyName: String get() = "C/C++"
  override val logo: Icon get() = EducationalCoreIcons.Language.Cpp
}
