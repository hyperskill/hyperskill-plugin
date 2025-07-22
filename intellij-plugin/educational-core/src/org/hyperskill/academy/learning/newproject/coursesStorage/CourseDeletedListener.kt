package org.hyperskill.academy.learning.newproject.coursesStorage

import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage

interface CourseDeletedListener {
  fun courseDeleted(course: JBACourseFromStorage)
}