package org.hyperskill.academy.scala.gradle

import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.learning.courseFormat.Course

open class ScalaGradleCourseBuilder : GradleCourseBuilderBase() {

  override fun buildGradleTemplateName(course: Course): String = SCALA_BUILD_GRADLE_TEMPLATE_NAME
  override fun taskTemplateName(course: Course): String = ScalaGradleConfigurator.TASK_SCALA
  override fun mainTemplateName(course: Course): String = ScalaGradleConfigurator.MAIN_SCALA
  override fun testTemplateName(course: Course): String = ScalaGradleConfigurator.TEST_SCALA

  companion object {
    private const val SCALA_BUILD_GRADLE_TEMPLATE_NAME = "scala-build.gradle"
  }
}
