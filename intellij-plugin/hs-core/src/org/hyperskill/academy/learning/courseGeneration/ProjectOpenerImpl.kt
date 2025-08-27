package org.hyperskill.academy.learning.courseGeneration

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.ui.JoinCourseDialog
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CourseDisplaySettings
import org.hyperskill.academy.learning.stepik.builtInServer.EduBuiltInServerUtils

class ProjectOpenerImpl : ProjectOpener() {

  override fun newProject(course: Course): Boolean {
    return JoinCourseDialog(
      course,
      CourseDisplaySettings(showTagsPanel = false, showInstructorField = false)
    ).showAndGet()
  }

  override fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>? =
    EduBuiltInServerUtils.focusOpenProject(coursePredicate)
}