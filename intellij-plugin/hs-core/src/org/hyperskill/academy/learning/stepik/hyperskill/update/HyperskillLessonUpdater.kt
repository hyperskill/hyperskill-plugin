package org.hyperskill.academy.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.LessonContainer
import org.hyperskill.academy.learning.update.FrameworkTaskUpdater
import org.hyperskill.academy.learning.update.HyperskillItemUpdater
import org.hyperskill.academy.learning.update.LessonUpdater
import org.hyperskill.academy.learning.update.TaskUpdater

class HyperskillLessonUpdater(
  project: Project,
  container: LessonContainer
) : LessonUpdater(project, container), HyperskillItemUpdater<Lesson> {
  override fun createTaskUpdater(lesson: Lesson): TaskUpdater = HyperskillTaskUpdater(project, lesson)
  override fun createFrameworkTaskUpdater(lesson: FrameworkLesson): FrameworkTaskUpdater = HyperskillFrameworkTaskUpdater(project, lesson)
}