package org.hyperskill.academy.scala.hyperskill

import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase.Companion.HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.document
import org.hyperskill.academy.scala.hyperskill.ScalaHyperskillConfigurator.Companion.SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME
import org.jetbrains.plugins.scala.ScalaLanguage
import org.junit.Test

class ScalaHyperskillCourseGenerationTest : EduTestCase() {
  @Test
  fun `test build gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = ScalaLanguage.INSTANCE
    ) {}
    val actualBuildGradleContent = findFile(DEFAULT_SCRIPT_NAME).document.text
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(SCALA_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME)

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }

  @Test
  fun `test settings gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = ScalaLanguage.INSTANCE
    ) {}
    val actualSettingsGradleContent = findFile(SETTINGS_FILE_NAME).document.text
    val expectedSettingsGradleContent = GeneratorUtils.getInternalTemplateText(HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME)

    assertEquals(expectedSettingsGradleContent, actualSettingsGradleContent)
  }
}