package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.Lesson
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.YAML_TEST_PROJECT_READY
import com.jetbrains.edu.learning.yaml.YamlFormatSettings.YAML_TEST_THROW_EXCEPTION
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.edu.learning.yaml.format.YamlMixinNames.HYPERSKILL_TYPE_YAML
import org.junit.Test

class YamlTypeChangedTest : YamlTestCase() {

  override fun setUp() {
    super.setUp()
    project.putUserData(YAML_TEST_PROJECT_READY, false)
  }

  @Test
  fun `test edu to hyperskill course`() {
    project.putUserData(YAML_TEST_THROW_EXCEPTION, false)
    testCourseTypeChanged(HYPERSKILL_TYPE_YAML, HyperskillCourse::class.java)
  }

  private fun <T : Course> testCourseTypeChanged(courseType: String, expectedCourse: Class<T>) {
    val course = getCourse()
    loadItemFromConfig(
      course, """
      |type: $courseType
      |title: Kotlin Course41
      |language: English
      |summary: test
      |programming_language: Plain text
      |content:
      |- lesson1
      |""".trimMargin()
    )

    val loadedCourse = getCourse()
    assertInstanceOf(loadedCourse, expectedCourse)
    assertEquals(course.items.size, loadedCourse.items.size)
  }

  private fun <T : Task> testTaskTypeChanged(type: String, expectedClass: Class<T>) {
    val task = findTask(0, 0)
    loadItemFromConfig(
      task, """
      |type: $type
      |feedback_link: http://example.com
      |files:
      |- name: test1.txt
      |""".trimMargin()
    )

    val loadedTask = findTask(0, 0)
    assertInstanceOf(loadedTask, expectedClass)
    assertEquals(1, loadedTask.index)
    assertEquals(1, loadedTask.taskFiles.size)
  }

  private fun <T : Lesson> testLessonTypeChanged(type: String, expectedClass: Class<T>) {
    val lesson = findLesson(0)
    loadItemFromConfig(
      lesson, """
      |type: $type
      |content:
      | - task1
      | - choice
      |""".trimMargin()
    )

    val loadedLesson = findLesson(0)
    assertInstanceOf(loadedLesson, expectedClass)
    assertEquals(1, loadedLesson.index)
    assertEquals(2, loadedLesson.items.size)
  }
}