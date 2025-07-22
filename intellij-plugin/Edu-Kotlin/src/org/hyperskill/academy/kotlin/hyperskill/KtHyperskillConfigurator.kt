package org.hyperskill.academy.kotlin.hyperskill

import com.google.common.annotations.VisibleForTesting
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.kotlin.KtConfigurator
import org.hyperskill.academy.kotlin.KtConfigurator.Companion.MAIN_KT
import org.hyperskill.academy.kotlin.KtCourseBuilder
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator

class KtHyperskillConfigurator : HyperskillConfigurator<JdkProjectSettings>(KtConfigurator()) {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = KtHyperskillCourseBuilder()

  override fun getMockFileName(course: Course, text: String): String = MAIN_KT

  private class KtHyperskillCourseBuilder : KtCourseBuilder() {
    override fun buildGradleTemplateName(course: Course): String = KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME
    override fun settingGradleTemplateName(course: Course): String = HYPERSKILL_SETTINGS_GRADLE_TEMPLATE_NAME
  }

  companion object {
    @VisibleForTesting
    const val KOTLIN_HYPERSKILL_BUILD_GRADLE_TEMPLATE_NAME = "hyperskill-kotlin-build.gradle"
  }
}
