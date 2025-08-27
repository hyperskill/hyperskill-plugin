package org.hyperskill.academy.python.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.RefreshCause
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.python.learning.newproject.PyCourseProjectGenerator
import org.hyperskill.academy.python.learning.newproject.PyLanguageSettings
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings

open class PyCourseBuilder : EduCourseBuilder<PyProjectSettings> {
  override fun taskTemplateName(course: Course): String? = PyConfigurator.TASK_PY
  override fun mainTemplateName(course: Course): String? = PyConfigurator.MAIN_PY
  override fun testTemplateName(course: Course): String? = PyConfigurator.TESTS_PY

  override fun getLanguageSettings(): LanguageSettings<PyProjectSettings> = PyLanguageSettings()

  override fun getSupportedLanguageVersions(): List<String> = getSupportedVersions()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<PyProjectSettings>? =
    PyCourseProjectGenerator(this, course)

  override fun refreshProject(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.DEPENDENCIES_UPDATED) {
      val projectSdk = ProjectRootManager.getInstance(project).projectSdk ?: return
      installRequiredPackages(project, projectSdk)
    }
  }
}
