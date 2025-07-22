package org.hyperskill.academy.learning.newproject.coursesStorage

import org.hyperskill.academy.learning.courseFormat.Course

interface CourseAddedListener {
  fun courseAdded(course: Course)
}