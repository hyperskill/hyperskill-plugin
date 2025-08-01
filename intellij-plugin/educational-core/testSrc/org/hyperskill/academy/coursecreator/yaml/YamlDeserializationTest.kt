package org.hyperskill.academy.coursecreator.yaml

import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.EduFileErrorHighlightLevel
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.ext.languageById
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.IdeTask
import org.hyperskill.academy.learning.courseFormat.tasks.OutputTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeCourse
import org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeLesson
import org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeSection
import org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeTask
import org.hyperskill.academy.learning.yaml.YamlDeserializer.getCourseMode
import org.hyperskill.academy.learning.yaml.YamlMapper.basicMapper
import org.hyperskill.academy.learning.yaml.YamlTestCase
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.HYPERSKILL_TYPE_YAML
import org.junit.Test


class YamlDeserializationTest : YamlTestCase() {

  @Test
  fun `test hyperskill course`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "Plain text"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"
    val yamlContent = """
      |type: $HYPERSKILL_TYPE_YAML
      |title: $name
      |language: $language
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: $programmingLanguage
      |content:
      |- $firstLesson
      |- $secondLesson
      |""".trimMargin()
    val course = basicMapper().deserializeCourse(yamlContent) as HyperskillCourse
    assertEquals(name, course.name)
    assertEquals(language, course.humanLanguage)
    assertEquals(programmingLanguage, course.languageById!!.displayName)
    assertEquals(listOf(firstLesson, secondLesson), course.items.map { it.name })
  }

  @Test
  fun `test section`() {
    val firstLesson = "Introduction Lesson"
    val secondLesson = "Advanced Lesson"
    val yamlContent = """
      |content:
      |- $firstLesson
      |- $secondLesson
    """.trimMargin()
    val section = basicMapper().deserializeSection(yamlContent)
    assertEquals(listOf(firstLesson, secondLesson), section.items.map { it.name })
  }

  @Test
  fun `test section with custom presentable name`() {
    val firstLesson = "Introduction Lesson"
    val secondLesson = "Advanced Lesson"
    val customSectionName = "custom section name"
    val yamlContent = """
      |custom_name: $customSectionName
      |content:
      |- $firstLesson
      |- $secondLesson
    """.trimMargin()
    val section = basicMapper().deserializeSection(yamlContent)
    assertEquals(listOf(firstLesson, secondLesson), section.items.map { it.name })
    @Suppress("DEPRECATION")
    assertEquals(customSectionName, section.customPresentableName)
  }

  @Test
  fun `test section with content tags`() {
    val firstLesson = "Introduction Lesson"
    val secondLesson = "Advanced Lesson"
    val contentTags = listOf("kotlin", "cycles")
    val yamlContent = """
      |content:
      |- $firstLesson
      |- $secondLesson
      |tags: $contentTags
    """.trimMargin()
    val section = basicMapper().deserializeSection(yamlContent)
    assertEquals(listOf(firstLesson, secondLesson), section.items.map { it.name })
    assertEquals(contentTags, section.contentTags)
  }

  @Test
  fun `test lesson`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
  }

  @Test
  fun `test lesson with custom presentable name`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val lessonCustomName = "my best lesson"
    val yamlContent = """
      |custom_name: $lessonCustomName
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
    @Suppress("DEPRECATION")
    assertEquals(lessonCustomName, lesson.customPresentableName)
  }

  @Test
  fun `test lesson with explicit type`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |type: lesson
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
  }

  @Test
  fun `test lesson with content tags`() {
    val lessonCustomName = "first lesson"
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val contentTags = listOf("kotlin", "cycles")
    val yamlContent = """
      |custom_name: $lessonCustomName
      |content:
      |- $firstTask
      |- $secondTask
      |tags: $contentTags
    """.trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    assertEquals(contentTags, lesson.contentTags)
  }

  @Test
  fun `test framework lesson`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val yamlContent = """
      |type: framework
      |content:
      |- $firstTask
      |- $secondTask
    """.trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    check(lesson is FrameworkLesson)
    assertEquals(listOf(firstTask, secondTask), lesson.taskList.map { it.name })
    assertTrue(lesson.isTemplateBased)
  }

  @Test
  fun `test framework lesson with content tags`() {
    val firstTask = "Introduction Task"
    val secondTask = "Advanced Task"
    val contentTags = listOf("kotlin", "cycles")
    val yamlContent = """
      |type: framework
      |content:
      |- $firstTask
      |- $secondTask
      |tags: $contentTags
    """.trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    check(lesson is FrameworkLesson)
    assertEquals(contentTags, lesson.contentTags)
  }

  @Test
  fun `test empty framework lesson`() {
    val yamlContent = """
      |type: framework
      |
    """.trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    assertTrue(lesson is FrameworkLesson)
  }

  @Test
  fun `test non templated based framework lesson`() {
    val yamlContent = """
      |type: framework
      |is_template_based: false
    """.trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    check(lesson is FrameworkLesson)
    assertFalse(lesson.isTemplateBased)
  }

  @Test
  fun `test output task`() {
    val yamlContent = """
    |type: output
    |solution_hidden: false
    |files:
    |- name: Test.java
    |""".trimMargin()
    val task = basicMapper().deserializeTask(yamlContent)
    assertTrue(task is OutputTask)
    assertEquals(listOf("Test.java"), task.taskFiles.map { it.key })
    assertEquals(false, task.solutionHidden)
  }

  @Test
  fun `test ide task`() {
    val yamlContent = """
    |type: ide
    |solution_hidden: true
    |files:
    |- name: Test.java
    |""".trimMargin()
    val task = basicMapper().deserializeTask(yamlContent)
    assertTrue(task is IdeTask)
    assertEquals(listOf("Test.java"), task.taskFiles.map { it.key })
    assertEquals(true, task.solutionHidden)
  }

  @Test
  fun `test remote edu task with check profile`() {
    val checkProfile = "hyperskill_go"
    val yamlContent = """
    |type: remote_edu
    |files:
    |- name: Test.java
    |check_profile: $checkProfile
    |""".trimMargin()
    val task = basicMapper().deserializeTask(yamlContent)
    assertTrue(task is RemoteEduTask)
    assertEquals((task as RemoteEduTask).checkProfile, checkProfile)
  }

  @Test
  fun `test with custom presentable name`() {
    val customName = "custom name"
    val yamlContent = """
    |type: edu
    |custom_name: $customName
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    dependency:
    |      lesson: lesson1
    |      task: task1
    |      file: Test.java
    |      placeholder: 1
    |      is_visible: true
    |""".trimMargin()
    val task = basicMapper().deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    @Suppress("DEPRECATION")
    assertEquals(customName, task.customPresentableName)
  }

  @Test
  fun `test edu task with content tags`() {
    val contentTags = listOf("kotlin", "cycles")
    val yamlContent = """
    |type: edu
    |custom_name: custom Name
    |files:
    |- name: Test.java
    |  placeholders:
    |  - offset: 0
    |    length: 3
    |    placeholder_text: type here
    |    dependency:
    |      lesson: lesson1
    |      task: task1
    |      file: Test.java
    |      placeholder: 1
    |      is_visible: true
    |tags: $contentTags
    |""".trimMargin()
    val task = basicMapper().deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals(contentTags, task.contentTags)
  }

  @Test
  fun `test edu task with turned off highlighting`() {
    val taskYaml = """
    |type: edu
    |files:
    |- name: A.java
    |  visible: true
    |  highlight_level: NONE
    |- name: B.java
    |  visible: true
    |  highlight_level: ALL_PROBLEMS
    |- name: C.java
    |  visible: true
    |""".trimMargin()
    val task = basicMapper().deserializeTask(taskYaml)
    assertEquals(task.taskFiles["A.java"]?.errorHighlightLevel, EduFileErrorHighlightLevel.NONE)
    assertEquals(task.taskFiles["B.java"]?.errorHighlightLevel, EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION)
    assertEquals(task.taskFiles["C.java"]?.errorHighlightLevel, EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION)
  }

  @Test
  fun `test feedback link`() {
    val yamlContent = """
    |type: edu
    |feedback_link: http://example.com
    |files:
    |- name: Test.java
    |""".trimMargin()
    val task = basicMapper().deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    assertEquals("http://example.com", task.feedbackLink)
  }

  @Test
  fun `test file visibility`() {
    val taskFileName = "Task.java"
    val testFileName = "Test.java"
    val yamlContent = """
    |type: edu
    |files:
    |- name: $taskFileName
    |  visible: true
    |- name: $testFileName
    |  visible: false
    |""".trimMargin()
    val task = basicMapper().deserializeTask(yamlContent)
    assertTrue(task is EduTask)
    val taskFile = task.taskFiles[taskFileName]!!
    assertTrue("$taskFileName expected to be visible", taskFile.isVisible)
    val testFile = task.taskFiles[testFileName]!!
    assertTrue("$testFileName expected to be invisible", !testFile.isVisible)
  }

  @Test
  fun `test empty edu task`() {
    val yamlContent = """
    |type: edu
    |""".trimMargin()
    val task = basicMapper().deserializeTask(yamlContent)
    assertEmpty(task.taskFiles.values)
  }

  @Test
  fun `test empty lesson`() {
    val yamlContent = """
    |
    |{}
    |""".trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    assertTrue(lesson.taskList.isEmpty())
  }

  @Test
  fun `test empty lesson with empty config`() {
    val yamlContent = """
    |""".trimMargin()
    val lesson = basicMapper().deserializeLesson(yamlContent)
    assertEmpty(lesson.taskList)
  }

  @Test
  fun `test empty section`() {
    val yamlContent = """
    |
    |{}
    |""".trimMargin()
    val section = basicMapper().deserializeSection(yamlContent)
    assertTrue(section.lessons.isEmpty())
  }

  @Test
  fun `test empty section with empty config`() {
    val yamlContent = """
    |""".trimMargin()
    val section = basicMapper().deserializeSection(yamlContent)
    assertEmpty(section.lessons)
  }

  @Test
  fun `test cc mode`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |""".trimMargin()

    assertNull(getCourseMode(yamlContent))
  }

  @Test
  fun `test study mode`() {
    val yamlContent = """
      |title: Test Course
      |language: Russian
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: Plain text
      |content:
      |- the first lesson
      |- the second lesson
      |mode: Study
      |""".trimMargin()

    assertEquals(CourseMode.STUDENT, getCourseMode(yamlContent))
  }

}