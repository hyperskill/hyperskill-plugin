package org.hyperskill.academy.learning.stepik.hyperskill

import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.findTask
import org.hyperskill.academy.learning.submissions.getSolutionFiles
import org.junit.Test

class HyperskillUploadingTest : EduTestCase() {
  @Test
  fun `test collect solution files`() {
    val course = createHyperskillCourse()
    val task = course.findTask("lesson1", "task1")
    val files = getSolutionFiles(project, task)
    assertEquals(listOf("src/Task.kt", "src/Baz.kt", "test/Tests1.kt", "test/VisibleTests.kt"), files.map { it.name })
    assertEquals(listOf(true, false, false, true), files.map { it.isVisible })
  }

  @Test
  fun `test collect solution files - test file with wrong visible=true is sent as is_visible=false`() {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}", visible = true)
          taskFile("test/Tests1.kt", "fun tests1() {}", visible = true) // wrong: visible=true for test file
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val task = course.findTask("lesson1", "task1")
    val files = getSolutionFiles(project, task)
    assertEquals(2, files.size)
    assertEquals(true, files.find { it.name == "src/Task.kt" }?.isVisible)
    assertEquals(false, files.find { it.name == "test/Tests1.kt" }?.isVisible)
  }

  private fun createHyperskillCourse(): HyperskillCourse {
    val course = courseWithFiles(
      language = FakeGradleBasedLanguage,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}", visible = true)
          taskFile("src/Baz.kt", "fun baz() {}", visible = false)
          taskFile("test/Tests1.kt", "fun tests1() {}", visible = false)
          taskFile("test/VisibleTests.kt", "fun visibleTests() {}", visible = true)
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))
    return course
  }
}
