package org.hyperskill.academy.learning.courseFormat

import org.hyperskill.academy.learning.courseFormat.tasks.Task

abstract class LessonContainer : ItemContainer() {
  val lessons: List<Lesson>
    get() = items.filterIsInstance<Lesson>()

  fun getLesson(name: String): Lesson? {
    return getLesson { it.name == name }
  }

  fun getLesson(id: Int): Lesson? {
    return getLesson { id == it.id }
  }

  fun getLesson(check: (Lesson) -> Boolean): Lesson? {
    return lessons.firstOrNull { check(it) }
  }

  fun addLesson(lesson: Lesson) {
    addItem(lesson)
  }

  fun removeLesson(lesson: Lesson) {
    removeItem(lesson)
  }

  fun visitLessons(visit: (Lesson) -> Unit) {
    for (item in items) {
      if (item is Lesson) {
        visit(item)
      }
      else if (item is Section) {
        for (lesson in item.lessons) {
          visit(lesson)
        }
      }
    }
  }

  fun visitSections(visit: (Section) -> Unit) {
    items.filterIsInstance<Section>().forEach(visit)
  }

  fun visitTasks(visit: (Task) -> Unit) {
    visitLessons { it.visitTasks(visit) }
  }
}