package org.hyperskill.academy.coursecreator.yaml

import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTopic
import org.hyperskill.academy.learning.yaml.YamlMapper
import org.hyperskill.academy.learning.yaml.YamlTestCase
import org.junit.Test
import java.text.SimpleDateFormat
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
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    course.updateDate = dateFormat.parse("Fri, 01 Jan 2010 00:00:00 UTC")

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
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    section.updateDate = dateFormat.parse("Fri, 01 Jan 2010 00:00:00 UTC")
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
    val dateFormat = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US)
    task.updateDate = dateFormat.parse("Fri, 01 Jan 2010 00:00:00 UTC")
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