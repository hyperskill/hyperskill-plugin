package org.hyperskill.academy.sql.jvm.gradle

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.navigation.TaskNavigationExtension

class SqlTaskNavigationExtension : TaskNavigationExtension {
  override fun onTaskNavigation(project: Project, task: Task, fromTask: Task?) {
    val lesson = task.lesson
    if (lesson is FrameworkLesson) {
      attachSqlConsoleForOpenFiles(project, task)
      // Navigation was performed from another task of the same framework lessons
      if (fromTask != null && fromTask.lesson == lesson) {
        executeInitScripts(project, listOf(task))
      }
    }
  }
}
