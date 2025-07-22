package org.hyperskill.academy.shell

import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings

class ShellCourseBuilder : EduCourseBuilder<EmptyProjectSettings> {
  override fun taskTemplateName(course: Course): String = ShellConfigurator.TASK_SH

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<EmptyProjectSettings> =
    ShellCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<EmptyProjectSettings> = ShellLanguageSettings()
}