package org.hyperskill.academy.kotlin

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.jvm.gradle.GradleConfiguratorBase
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.kotlin.checker.KtTaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import javax.swing.Icon

class KtConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: GradleCourseBuilderBase
    get() = KtCourseBuilder()

  override val testFileName: String
    get() = TESTS_KT

  override fun getMockFileName(course: Course, text: String): String = TASK_KT

  override val taskCheckerProvider: KtTaskCheckerProvider
    get() = KtTaskCheckerProvider()

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_KT)

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Kotlin

  override val defaultPlaceholderText: String
    get() = "TODO()"

  companion object {
    const val TESTS_KT = "Tests.kt"
    const val TASK_KT = "Task.kt"
    const val MAIN_KT = "Main.kt"
    const val MOCK_KT = "Mock.kt"
  }
}
