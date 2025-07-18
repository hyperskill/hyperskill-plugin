package com.jetbrains.edu.coursecreator.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillStage
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import com.jetbrains.edu.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import com.jetbrains.edu.learning.yaml.YamlDeserializer
import com.jetbrains.edu.learning.yaml.YamlTestCase
import com.jetbrains.edu.learning.yaml.format.RemoteStudyItem
import org.junit.Test
import java.util.*

class YamlRemoteDeserializationTest : YamlTestCase() {
  @Test
  fun `test hyperskill project`() {
    val id = 15
    val ideFiles = "ideFiles"
    val isTemplateBased = true
    val yamlContent = """
      |hyperskill_project:
      |  id: $id
      |  ide_files: $ideFiles
      |  is_template_based: $isTemplateBased
      |update_date: Thu, 01 Jan 1970 00:00:00 UTC
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

    val configFile = createConfigFile(yamlContent, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as HyperskillCourse

    val hyperskillProject = course.hyperskillProject!!
    assertEquals(id, hyperskillProject.id)
    assertEquals(ideFiles, hyperskillProject.ideFiles)
    assertEquals(isTemplateBased, hyperskillProject.isTemplateBased)

    assertEquals(Date(0), course.updateDate)

    checkStage(HyperskillStage(1, "", 11, true), course.stages[0])
    checkStage(HyperskillStage(2, "", 22, false), course.stages[1])

    val hyperskillTopic = course.taskToTopics[0]!!.first()
    assertEquals(404, hyperskillTopic.theoryId)
    assertEquals("Learn Anything", hyperskillTopic.title)
  }

  private fun checkStage(expected: HyperskillStage, actual: HyperskillStage) {
    assertEquals(expected.id, actual.id)
    assertEquals(expected.stepId, actual.stepId)
    assertEquals(expected.isCompleted, actual.isCompleted)
  }

  @Test
  fun `test section`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_SECTION_CONFIG)
    val section = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile))
    assertEquals(1, section.id)
    assertEquals(Date(0), section.updateDate)
  }

  @Test
  fun `test task`() {
    val yamlText = """
    |id: 1
    |update_date: Thu, 01 Jan 1970 00:00:00 UTC
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_TASK_CONFIG)
    val task = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as RemoteStudyItem
    assertEquals(1, task.id)
    assertEquals(Date(0), task.updateDate)
  }

  @Test
  fun `test quoted date`() {
    val id = 1
    val yamlText = """
    |id: $id
    |update_date: "Thu, 01 Jan 1970 00:00:01 UTC"
    |""".trimMargin()

    val configFile = createConfigFile(yamlText, REMOTE_COURSE_CONFIG)
    val course = YamlDeserializer.deserializeRemoteItem(configFile.name, VfsUtil.loadText(configFile)) as HyperskillCourse
    assertEquals(1, course.id)
    assertEquals(Date(1000), course.updateDate)
  }

  private fun createConfigFile(yamlText: String, configName: String): LightVirtualFile {
    val configFile = LightVirtualFile(configName)
    runWriteAction { VfsUtil.saveText(configFile, yamlText) }
    return configFile
  }
}