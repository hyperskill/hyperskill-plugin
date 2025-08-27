package org.hyperskill.academy.sql.jvm.gradle

import org.hyperskill.academy.coursecreator.actions.TemplateFileInfo
import org.hyperskill.academy.coursecreator.actions.studyItem.NewStudyItemInfo
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.jvm.gradle.generation.GradleCourseProjectGenerator
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.sql.core.SqlConfiguratorBase

class SqlGradleCourseBuilder : GradleCourseBuilderBase() {
  override fun taskTemplateName(course: Course): String = SqlConfiguratorBase.TASK_SQL

  override fun getDefaultTaskTemplates(
    course: Course,
    info: NewStudyItemInfo,
    withSources: Boolean,
    withTests: Boolean
  ): List<TemplateFileInfo> {
    val templates = super.getDefaultTaskTemplates(course, info, withSources, withTests)
    return if (withSources) {
      templates + TemplateFileInfo(INIT_SQL, INIT_SQL, isVisible = false)
    }
    else {
      templates
    }
  }

  override fun getCourseProjectGenerator(course: Course): GradleCourseProjectGenerator {
    return SqlGradleCourseProjectGenerator(this, course)
  }

  override fun buildGradleTemplateName(course: Course): String {
    return when (course.sqlTestLanguage) {
      SqlTestLanguage.KOTLIN -> "sql-kotlin-build.gradle"
      SqlTestLanguage.JAVA -> "sql-java-build.gradle"
    }
  }

  override fun testTemplateName(course: Course): String {
    return when (course.sqlTestLanguage) {
      SqlTestLanguage.KOTLIN -> "SqlTest.kt"
      SqlTestLanguage.JAVA -> "SqlTest.java"
    }
  }

  override fun getLanguageSettings(): LanguageSettings<JdkProjectSettings> = SqlJdkLanguageSettings()

  companion object {
    const val INIT_SQL = "init.sql"
  }
}

