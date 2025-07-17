package com.jetbrains.edu.learning.format

import com.intellij.testFramework.LightPlatformTestCase
import com.jetbrains.edu.learning.EduTestCase
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.EduTask
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceOptionStatus
import com.jetbrains.edu.learning.courseFormat.tasks.choice.ChoiceTask
import com.jetbrains.edu.learning.createCourseFiles
import com.jetbrains.edu.learning.createCourseFromJson
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
  fun testFeedbackLinks() {
    assertEquals("https://www.jetbrains.com/", firstEduTask.feedbackLink)
  }

  @Test
  fun testPlaceholderText() {
    val taskFile = firstEduTask.getTaskFile("task.py")
    check(taskFile != null)
    val answerPlaceholders = taskFile.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("write function body", answerPlaceholders[0].placeholderText)
  }

  @Test
  fun testPossibleAnswer() {
    val taskFile = firstEduTask.getTaskFile("task.py")
    check(taskFile != null)
    val answerPlaceholders = taskFile.answerPlaceholders
    assertEquals(1, answerPlaceholders.size)
    assertEquals("pass", answerPlaceholders[0].possibleAnswer)
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
  fun testChoiceTasks() {
    val task = courseFromJson.lessons[0].taskList[0]
    check(task is ChoiceTask)
    assertTrue(task.isMultipleChoice)
    val choiceOptions = task.choiceOptions

    val actualChoiceOptions = choiceOptions.associateBy({ it.text }, { it.status })
    assertEquals(mapOf(Pair("1", ChoiceOptionStatus.CORRECT), Pair("2", ChoiceOptionStatus.INCORRECT)), actualChoiceOptions)
  }

  @Test
  fun testSolutionHiddenInTask() {
    val task = courseFromJson.lessons[0].taskList[0]
    assertTrue(task.solutionHidden!!)
  }

  @Test
  fun testPlaceholderWithInvisibleDependency() = doTestPlaceholderAndDependencyVisibility(
    courseFromJson.lessons[0].taskList[0],
    expectedPlaceholderVisibility = false
  )

  @Test
  fun testInvisiblePlaceholder() = doTestPlaceholderAndDependencyVisibility(
    courseFromJson.lessons[0].taskList[0],
    expectedPlaceholderVisibility = false
  )

  @Test
  fun testVisiblePlaceholderAndInvisibleDependency() = doTestPlaceholderAndDependencyVisibility(
    courseFromJson.lessons[0].taskList[0],
    expectedPlaceholderVisibility = false
  )

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