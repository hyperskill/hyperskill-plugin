package org.hyperskill.academy.csharp.hyperskill

import com.intellij.openapi.project.Project
import org.hyperskill.academy.csharp.CSharpLanguageSettings
import org.hyperskill.academy.csharp.CSharpProjectSettings
import org.hyperskill.academy.csharp.includeTopLevelDirsInCourseView
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.RefreshCause
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator

class CSharpHyperskillCourseBuilder : EduCourseBuilder<CSharpProjectSettings> {
  override fun getLanguageSettings(): LanguageSettings<CSharpProjectSettings> = CSharpLanguageSettings()

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<CSharpProjectSettings> =
    CSharpHyperskillProjectGenerator(this, course)

  /**
   * Is needed to index top-level files when the Unity project is re-opened as a solution
   */
  override fun refreshProject(project: Project, cause: RefreshCause) {
    super.refreshProject(project, cause)
    val course = project.course ?: error("No course associated with project")

    if (cause == RefreshCause.STRUCTURE_MODIFIED) {
      includeTopLevelDirsInCourseView(project, course)
    }
  }
}