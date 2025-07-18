package com.jetbrains.edu.learning.format

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.createCourseFiles
import com.jetbrains.edu.learning.createCourseFromJson
import org.junit.Test

class CourseFormatTest : EduTestCase() {
  @Test
  fun testAdditionalMaterialsLesson() {
    assertNotNull(courseFromJson.additionalFiles)
    assertFalse(courseFromJson.additionalFiles.isEmpty())
    assertEquals("test_helper.py", courseFromJson.additionalFiles[0].name)
  }

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
  fun testFeedbackLinks() {
    assertEquals("https://www.jetbrains.com/", firstEduTask.feedbackLink)
  }

  @Test
  fun testCourseName() {
    assertEquals("My Python Course", courseFromJson.name)
  }

  @Test
  fun testCourseDescription() {
    assertEquals("Best course ever", courseFromJson.description)
  }

  @Test
  fun testStudentTaskText() {
    val lessons = courseFromJson.lessons
    assertFalse("No lessons found", lessons.isEmpty())
    val lesson = lessons[0]
    val taskList = lesson.taskList
    assertFalse("No tasks found", taskList.isEmpty())
    val task = taskList[0]
    val taskFile = task.getTaskFile("my_task.py")
    assertNotNull(taskFile)
    assertEquals("def foo():\n    write function body\n", taskFile!!.text)
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
      return createCourseFromJson(testDataPath + fileName, CourseMode.STUDENT)
    }

  override fun getTestDataPath(): String = "${super.getTestDataPath()}/format/"

  private val testFile: String get() = "${getTestName(true)}.json"

  private val firstEduTask: EduTask
    get() {
      val course = courseFromJson
      course.init(false)
      course.createCourseFiles(project, LightPlatformTestCase.getSourceRoot())
      val lessons = course.lessons
      assertFalse("No lessons found", lessons.isEmpty())
      val lesson = lessons[0]
      val taskList = lesson.taskList
      assertFalse("No tasks found", taskList.isEmpty())
      val task = taskList[0]
      check(task is EduTask)
      return task
    }
}