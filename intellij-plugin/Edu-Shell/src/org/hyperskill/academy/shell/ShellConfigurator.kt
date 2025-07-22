package org.hyperskill.academy.shell

import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings
import org.jetbrains.annotations.NonNls
import javax.swing.Icon

class ShellConfigurator : EduConfigurator<EmptyProjectSettings> {

  override val courseBuilder: EduCourseBuilder<EmptyProjectSettings>
    get() = ShellCourseBuilder()

  override val testFileName: String
    get() = TEST_SH

  override val taskCheckerProvider: TaskCheckerProvider
    get() = ShellTaskCheckerProvider()

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Shell

  override fun getMockFileName(course: Course, text: String): String = TASK_SH

  companion object {
    @NonNls
    const val TASK_SH = "task.sh"

    @NonNls
    const val TEST_SH = "test.sh"
  }
}