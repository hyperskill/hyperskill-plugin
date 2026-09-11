package org.hyperskill.academy.java.hyperskill

import com.google.common.annotations.VisibleForTesting
import com.intellij.openapi.projectRoots.Sdk
import org.hyperskill.academy.java.JConfigurator
import org.hyperskill.academy.java.JCourseBuilder
import org.hyperskill.academy.jvm.JdkLanguageSettings
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.jvm.gradle.GradleHyperskillConfigurator
import org.hyperskill.academy.jvm.gradle.generation.GradleCourseProjectGenerator
import org.hyperskill.academy.jvm.requiredJdkVersion
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillCourseBuilder
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillCourseProjectGenerator

class JHyperskillConfigurator : GradleHyperskillConfigurator<JdkProjectSettings>(JConfigurator()) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = JHyperskillCourseBuilder(JHyperskillGradleCourseBuilder())

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST, "${EduNames.TEST}/stageTest")

  private class JHyperskillGradleCourseBuilder : JCourseBuilder() {
    override fun buildGradleTemplateName(course: Course): String = JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    override fun settingGradleTemplateName(course: Course): String = HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
    override fun getLanguageSettings(): JHyperskillLanguageSettings = JHyperskillLanguageSettings()
  }

  private class JHyperskillCourseBuilder(private val gradleCourseBuilder: GradleCourseBuilderBase) :
    HyperskillCourseBuilder<JdkProjectSettings>(gradleCourseBuilder) {

    override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JdkProjectSettings>? {
      if (course !is HyperskillCourse) return null
      val generatorBase = JHyperskillCourseProjectGenerator(gradleCourseBuilder, course)
      return HyperskillCourseProjectGenerator(generatorBase, this, course)
    }
  }

  private class JHyperskillCourseProjectGenerator(builder: GradleCourseBuilderBase, course: Course) :
    GradleCourseProjectGenerator(builder, course) {

    override fun getJdk(settings: JdkProjectSettings): Sdk? {
      // `course.languageVersion` is the language level of a Hyperskill Java course ("11"), not the JDK its checker
      // needs, so it must not be used here: it would accept a JDK the course cannot be checked with
      return super.getJdk(settings) ?: JdkLanguageSettings.findSuitableJdk(course.requiredJdkVersion, settings.model)
    }
  }

  companion object {
    @VisibleForTesting
    const val JAVA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME = "hyperskill-java-build.gradle"
  }
}
