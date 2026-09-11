package org.hyperskill.academy.learning.courseFormat

import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.junit.Assert.assertSame
import org.junit.Test

class ItemContainerTest {

  @Test
  fun `test adding a task makes the lesson its parent`() {
    val lesson = Lesson().apply { name = "lesson1" }
    val task = EduTask("task1")

    lesson.addTask(task)

    assertSame(lesson, task.parentOrNull)
  }

  @Test
  fun `test adding a task at an index makes the lesson its parent`() {
    val lesson = Lesson().apply { name = "lesson1" }
    val first = EduTask("task1")
    lesson.addTask(first)
    val second = EduTask("task2")

    lesson.addTask(0, second)

    assertSame(lesson, second.parentOrNull)
  }

  @Test
  fun `test replacing an item makes the container its parent`() {
    val lesson = Lesson().apply { name = "lesson1" }
    val existing = EduTask("task1")
    lesson.addTask(existing)
    val replacement = EduTask("task1")

    lesson.replaceItem(existing, replacement)

    assertSame(lesson, replacement.parentOrNull)
  }

  @Test
  fun `test adding a lesson makes the section its parent`() {
    val section = Section().apply { name = "section1" }
    val lesson = Lesson().apply { name = "lesson1" }

    section.addLesson(lesson)

    assertSame(section, lesson.parentOrNull)
  }
}
