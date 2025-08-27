package org.hyperskill.academy.php

import com.intellij.openapi.project.Project
import com.jetbrains.php.composer.actions.ComposerAbstractAction
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.RefreshCause
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator

class PhpCourseBuilder : EduCourseBuilder<PhpProjectSettings> {
  override fun taskTemplateName(course: Course): String = PhpConfigurator.TASK_PHP
  override fun mainTemplateName(course: Course): String = PhpConfigurator.MAIN_PHP
  override fun testTemplateName(course: Course): String = PhpConfigurator.TEST_PHP

  override fun getLanguageSettings(): LanguageSettings<PhpProjectSettings> = PhpLanguageSettings()

  override fun getCourseProjectGenerator(course: Course):
    CourseProjectGenerator<PhpProjectSettings> = PhpCourseProjectGenerator(this, course)

  override fun refreshProject(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.DEPENDENCIES_UPDATED) {
      ComposerAbstractAction.refreshConfigAndLockFiles(project.courseDir, null)
    }
  }
}