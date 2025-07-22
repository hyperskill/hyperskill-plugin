package org.hyperskill.academy.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.update.FrameworkTaskUpdater
import org.hyperskill.academy.learning.update.HyperskillItemUpdater
import org.hyperskill.academy.learning.update.TaskUpdater

class HyperskillTaskUpdater(project: Project, lesson: Lesson) :
  TaskUpdater(project, lesson),
  HyperskillItemUpdater<Task>

class HyperskillFrameworkTaskUpdater(project: Project, lesson: FrameworkLesson) :
  FrameworkTaskUpdater(project, lesson),
  HyperskillItemUpdater<Task> {

  override fun Task.canBeUpdatedBy(remoteTask: Task): Boolean = true

  override suspend fun Task.shouldBeUpdated(remoteTask: Task): Boolean = isOutdated(remoteTask)
}