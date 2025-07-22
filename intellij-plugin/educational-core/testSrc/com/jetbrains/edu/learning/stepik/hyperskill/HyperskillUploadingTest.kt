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
    assertEquals(3, files.size)
    for (file in files) {
      assertEquals(mapOf("src/Task.kt" to true, "src/Baz.kt" to false, "test/Tests1.kt" to false)[file.name], file.isVisible)
    }
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
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))
    return course
  }
}
