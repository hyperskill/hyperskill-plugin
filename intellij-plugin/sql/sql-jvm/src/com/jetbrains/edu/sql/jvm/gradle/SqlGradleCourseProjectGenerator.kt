package org.hyperskill.academy.sql.jvm.gradle

import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.jvm.gradle.generation.GradleCourseProjectGenerator
import org.hyperskill.academy.learning.courseFormat.Course

class SqlGradleCourseProjectGenerator(
  builder: GradleCourseBuilderBase,
  course: Course
) : GradleCourseProjectGenerator(builder, course) {

  override fun applySettings(projectSettings: JdkProjectSettings) {
    super.applySettings(projectSettings)
    if (projectSettings is SqlJdkProjectSettings && projectSettings.testLanguage != null) {
      course.sqlTestLanguage = projectSettings.testLanguage
    }
  }
}
