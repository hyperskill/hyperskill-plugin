package org.hyperskill.academy.learning.format

import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.createCourseFromJson
import org.junit.Test

class CourseFormatTest : EduTestCase() {
  @Test
  fun testCourseWithSection() {
    val items = courseFromJson.items
    assertEquals(2, items.size)
    assertTrue(items[0] is Section)
    assertTrue(items[1] is Lesson)
    assertEquals(1, (items[0] as Section).lessons.size)
  }

  @Test
  fun testFrameworkLesson() {
    assertEquals(1, courseFromJson.items.size)
    val lesson = courseFromJson.items[0]
    check(lesson is FrameworkLesson)
    assertTrue(lesson.isTemplateBased)
  }

  @Test
  fun testNonTemplateBasedFrameworkLesson() {
    assertEquals(1, courseFromJson.items.size)
    val lesson = courseFromJson.items[0]
    check(lesson is FrameworkLesson)
    assertFalse(lesson.isTemplateBased)
  }

  @Test
  fun testPycharmToEduTask() {
    val lessons = courseFromJson.lessons
    assertFalse("No lessons found", lessons.isEmpty())
    val lesson = lessons[0]
    val taskList = lesson.taskList
    assertFalse("No tasks found", taskList.isEmpty())
    assertTrue(taskList[0] is EduTask)
  }

  @Test
  fun testSolutionHiddenInTask() {
    val task = courseFromJson.lessons[0].taskList[0]
    assertTrue(task.solutionHidden!!)
  }

  @Test
  fun testCourseLanguageVersionEmpty() {
    val course = course {}
    course.languageId = "Python"
    assertNull(course.languageVersion)
  }

  private val courseFromJson: Course
    get() {
      val fileName = testFile
      return createCourseFromJson(testDataPath + fileName)
    }

  override fun getTestDataPath(): String = "${super.getTestDataPath()}/format/"

  private val testFile: String get() = "${getTestName(true)}.json"

}