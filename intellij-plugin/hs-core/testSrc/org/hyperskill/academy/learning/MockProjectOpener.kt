package org.hyperskill.academy.learning

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseGeneration.ProjectOpener

class MockProjectOpener : ProjectOpener() {
  var project: Project? = null
    set(value) {
      field = value
      courseOpenedInNewProject = null
    }

  /**
   * The course passed to [newProject] during the last `open` call.
   * Unlike production, the mock generates course files into the current test project
   * and never replaces `project.course`, so this is the only way to inspect
   * a course "opened in a new project".
   */
  var courseOpenedInNewProject: Course? = null
    private set

  override fun newProject(course: Course): Boolean {
    assertProject()
    courseOpenedInNewProject = course
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