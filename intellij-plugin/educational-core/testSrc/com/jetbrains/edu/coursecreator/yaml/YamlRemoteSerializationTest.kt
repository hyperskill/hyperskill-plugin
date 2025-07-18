package com.jetbrains.edu.coursecreator.yaml

import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillProject
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillTopic
import com.jetbrains.edu.learning.yaml.YamlMapper
import com.jetbrains.edu.learning.yaml.YamlTestCase
import org.junit.Test
import java.util.*

class YamlRemoteSerializationTest : YamlTestCase() {
  @Test
  fun `test hyperskill project`() {
    val course = course(courseProducer = ::HyperskillCourse) { } as HyperskillCourse
    val hyperskillProject = HyperskillProject().apply {
      id = 111
      ideFiles = "ideFiles"
      isTemplateBased = true
    }
    course.hyperskillProject = hyperskillProject
    course.updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC")

    course.stages = listOf(
      HyperskillStage(1, "First", 11, true),
      HyperskillStage(2, "Second", 22)
    )

    val topic = HyperskillTopic()
    topic.title = "Learn Anything"
    topic.theoryId = 404
    course.taskToTopics = mutableMapOf(0 to listOf(topic))

    val expectedYaml = """
      |hyperskill_project:
      |  id: ${hyperskillProject.id}
      |  ide_files: ${hyperskillProject.ideFiles}
      |  is_template_based: ${hyperskillProject.isTemplateBased}
      |  use_ide: true
      |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
      |stages:
      |- id: 1
      |  step: 11
      |  is_completed: true
      |- id: 2
      |  step: 22
      |  is_completed: false
      |topics:
      |  0:
      |  - title: Learn Anything
      |    theory_id: 404
      |
    """.trimMargin()

    doTest(course, expectedYaml)
  }

  @Test
  fun `test section`() {
    val section = course {
      section()
    }.sections.first()

    section.id = 1
    section.updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC")
    doTest(
      section, """
    |id: 1
    |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
    |""".trimMargin()
    )
  }

  @Test
  fun `test task`() {
    val task = course {
      lesson {
        eduTask()
      }
    }.lessons.first().taskList.first()

    task.id = 1
    task.updateDate = Date("Fri, 01 Jan 2010 00:00:00 UTC")
    doTest(
      task, """
    |id: 1
    |update_date: "Fri, 01 Jan 2010 00:00:00 UTC"
    |""".trimMargin()
    )
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = YamlMapper.remoteMapper().writeValueAsString(item)
    assertEquals(expected, actual)
  }
}