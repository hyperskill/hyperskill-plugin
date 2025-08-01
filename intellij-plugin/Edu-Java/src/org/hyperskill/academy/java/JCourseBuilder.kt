package org.hyperskill.academy.java

import com.intellij.openapi.projectRoots.JavaSdkVersion
import org.hyperskill.academy.java.JLanguageSettings.Companion.DEFAULT_JAVA
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.learning.courseFormat.Course

open class JCourseBuilder : GradleCourseBuilderBase() {

  override fun buildGradleTemplateName(course: Course): String = JAVA_BUILD_GRADLE_TEMPLATE_NAME
  override fun taskTemplateName(course: Course): String = JConfigurator.TASK_JAVA
  override fun mainTemplateName(course: Course): String = JConfigurator.MAIN_JAVA
  override fun testTemplateName(course: Course): String = JConfigurator.TEST_JAVA

  override fun getSupportedLanguageVersions(): List<String> = JavaSdkVersion.values().filter {
    it.isAtLeast(DEFAULT_JAVA)
  }.map { it.description }

  override fun getLanguageSettings() = JLanguageSettings()

  companion object {
    const val JAVA_BUILD_GRADLE_TEMPLATE_NAME = "java-build.gradle"
  }
}
