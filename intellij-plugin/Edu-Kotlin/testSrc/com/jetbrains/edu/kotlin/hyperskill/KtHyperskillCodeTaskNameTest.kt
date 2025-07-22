package org.hyperskill.academy.kotlin.hyperskill

import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.junit.Test

class KtHyperskillCodeTaskNameTest : EduTestCase() {
  @Test
  fun `test find taskFile for uploading`() {
    val course = courseWithFiles(
      language = KotlinLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse
    ) {
      frameworkLesson("lesson1") {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt", "fun foo() {}")
          taskFile(
            "src/Main.kt", """
            fun main(args: Array<String>) {
              println("Hello World!")
            }
          """.trimIndent()
          )
          taskFile("test/Tests1.kt")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val task = findTask(0, 0)
    val configurator = course.configurator as HyperskillConfigurator
    val codeTaskFile = configurator.getCodeTaskFile(project, task)

    assertEquals("src/Main.kt", codeTaskFile!!.name)
  }

  @Test
  fun `test create name for taskfile`() {
    val course = courseWithFiles(
      language = KotlinLanguage.INSTANCE,
      courseProducer = ::HyperskillCourse
    ) {} as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1))

    val fileName = course.configurator?.getMockFileName(course, "")

    assertEquals("Main.kt", fileName)
  }
}