package org.hyperskill.academy.learning.newproject

import org.hyperskill.academy.learning.courseFormat.Course

data class CourseCreationInfo(
  val course: Course,
  val location: String?,
  val projectSettings: EduProjectSettings?
)
