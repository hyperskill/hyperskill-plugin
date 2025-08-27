package org.hyperskill.academy.learning.newproject.ui.coursePanel

import org.hyperskill.academy.learning.courseFormat.Course

data class CourseBindData(
  val course: Course,
  val displaySettings: CourseDisplaySettings = CourseDisplaySettings()
)
