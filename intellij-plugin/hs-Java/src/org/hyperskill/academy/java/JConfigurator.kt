package org.hyperskill.academy.java

import com.intellij.lang.java.JavaLanguage
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.jvm.gradle.GradleConfiguratorBase
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.jvm.gradle.checker.GradleTaskCheckerProvider
import org.hyperskill.academy.jvm.stepik.fileName
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import javax.swing.Icon

class JConfigurator : GradleConfiguratorBase() {
  override val courseBuilder: GradleCourseBuilderBase
    get() = JCourseBuilder()

  override val testFileName: String
    get() = TEST_JAVA

  override val isEnabled: Boolean
    get() = !EduUtilsKt.isAndroidStudio()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()

  override fun getMockFileName(course: Course, text: String): String = fileName(JavaLanguage.INSTANCE, text)

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_JAVA)

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Java

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val TEST_JAVA = "Tests.java"
    const val TASK_JAVA = "Task.java"
    const val MAIN_JAVA = "Main.java"
    const val MOCK_JAVA = "Mock.java"
  }
}