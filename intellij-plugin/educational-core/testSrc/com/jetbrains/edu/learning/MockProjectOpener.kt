package org.hyperskill.academy.learning

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseGeneration.ProjectOpener

class MockProjectOpener : ProjectOpener() {
  var project: Project? = null

  override fun newProject(course: Course): Boolean {
    assertProject()
    course.configurator?.beforeCourseStarted(course)
    course.createCourseFiles(project!!)
    return true
  }

  override fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? {
    assertProject()
    val course = project!!.course ?: return null
    return if (coursePredicate(course)) project!! to course else null
  }

  private fun assertProject() {
    project ?: error("Set up project explicitly in tests")
  }
}