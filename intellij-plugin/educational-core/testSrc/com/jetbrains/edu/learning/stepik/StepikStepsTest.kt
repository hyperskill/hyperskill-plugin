package org.hyperskill.academy.learning.stepik

import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseFormat.DescriptionFormat
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.findTask
import org.junit.Test

class StepikStepsTest : EduTestCase() {
  @Test
  fun `test no conversion for hyperskill course`() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      lesson {
        eduTask(
          taskDescription = "## This is the code:\n ```\n version \n ```\n This was the code.",
          taskDescriptionFormat = DescriptionFormat.MD
        )
      }
    }
    val task = course.findTask("lesson1", "task1")
    val step = Step(project, task)

    assertEquals(
      "## This is the code:\n ```\n version \n ```\n This was the code.".trimIndent(),
      step.text.trimIndent()
    )

  }
}
