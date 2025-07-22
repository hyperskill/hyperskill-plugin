package org.hyperskill.academy.learning.newproject.ui.coursePanel.groups

import org.hyperskill.academy.learning.courseFormat.Course

data class CoursesGroup(
  val name: String = "",
  val courses: List<Course>
) {
  companion object {
    fun fromCourses(courses: List<Course>): List<CoursesGroup> =
      listOf(CoursesGroup(courses = courses))

  }
}