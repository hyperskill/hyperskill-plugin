package org.hyperskill.academy.learning

import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import org.hyperskill.academy.learning.courseFormat.Course

interface CourseUpdateListener {
  fun courseUpdated(project: Project, course: Course)

  companion object {
    val COURSE_UPDATE: Topic<CourseUpdateListener> = Topic.create("COURSE_UPDATE", CourseUpdateListener::class.java)
  }
}
