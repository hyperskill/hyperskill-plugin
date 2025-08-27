package org.hyperskill.academy.scala.hyperskill

import com.google.common.annotations.VisibleForTesting
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.jvm.gradle.GradleHyperskillConfigurator
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.scala.gradle.ScalaGradleConfigurator
import org.hyperskill.academy.scala.gradle.ScalaGradleCourseBuilder

class ScalaHyperskillConfigurator : GradleHyperskillConfigurator<JdkProjectSettings>(ScalaGradleConfigurator()) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = ScalaHyperskillCourseBuilder()

  private class ScalaHyperskillCourseBuilder : ScalaGradleCourseBuilder() {
    override fun buildGradleTemplateName(course: Course): String = SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    override fun settingGradleTemplateName(course: Course): String = HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
  }

  companion object {
    @VisibleForTesting
    const val SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME = "hyperskill-scala-build.gradle"
  }
}
