package org.hyperskill.academy.kotlin.hyperskill

import com.intellij.util.ThrowableRunnable
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase.Companion.HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase.Companion.getKotlinTemplateVariables
import org.hyperskill.academy.kotlin.hyperskill.KtHyperskillConfigurator.Companion.KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.document
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.plugins.gradle.util.GradleConstants.DEFAULT_SCRIPT_NAME
import org.jetbrains.plugins.gradle.util.GradleConstants.SETTINGS_FILE_NAME
import org.junit.Test

class KtHyperskillCourseGenerationTest : EduTestCase() {
  override fun runTestRunnable(context: ThrowableRunnable<Throwable>) {
    withDefaultHtmlTaskDescription {
      super.runTestRunnable(context)
    }
  }

  @Test
  fun `test build gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = KotlinLanguage.INSTANCE
    ) {}
    val actualBuildGradleContent = findFile(DEFAULT_SCRIPT_NAME).document.text
    val expectedBuildGradleContent = GeneratorUtils.getInternalTemplateText(
      KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME,
      getKotlinTemplateVariables()
    )

    assertEquals(expectedBuildGradleContent, actualBuildGradleContent)
  }

  @Test
  fun `test settings gradle file`() {
    courseWithFiles(
      courseProducer = ::HyperskillCourse,
      language = KotlinLanguage.INSTANCE
    ) {}
    val actualSettingsGradleContent = findFile(SETTINGS_FILE_NAME).document.text
    val expectedSettingsGradleContent = GeneratorUtils.getInternalTemplateText(HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME)

    assertEquals(expectedSettingsGradleContent, actualSettingsGradleContent)
  }
}