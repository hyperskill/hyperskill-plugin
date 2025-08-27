package org.hyperskill.academy.learning

import org.hyperskill.academy.learning.courseFormat.Course


interface CourseSetListener {
  fun courseSet(course: Course)
}