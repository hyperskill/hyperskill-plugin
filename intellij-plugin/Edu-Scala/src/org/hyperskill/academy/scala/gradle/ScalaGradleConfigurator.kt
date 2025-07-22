package org.hyperskill.academy.scala.gradle

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.jvm.gradle.GradleConfiguratorBase
import org.hyperskill.academy.jvm.gradle.checker.GradleTaskCheckerProvider
import org.hyperskill.academy.jvm.stepik.fileName
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import org.hyperskill.academy.scala.isScalaPluginCompatible
import org.jetbrains.plugins.scala.ScalaLanguage
import javax.swing.Icon

class ScalaGradleConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: ScalaGradleCourseBuilder
    get() = ScalaGradleCourseBuilder()

  override val testFileName: String
    get() = TEST_SCALA

  override val isEnabled: Boolean
    get() = !EduUtilsKt.isAndroidStudio() && isScalaPluginCompatible

  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_SCALA)

  override fun getMockFileName(course: Course, text: String): String = fileName(ScalaLanguage.INSTANCE, text)

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Scala

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val TEST_SCALA = "Test.scala"
    const val TASK_SCALA = "Task.scala"
    const val MAIN_SCALA = "Main.scala"
    const val MOCK_SCALA = "Mock.scala"
  }
}
