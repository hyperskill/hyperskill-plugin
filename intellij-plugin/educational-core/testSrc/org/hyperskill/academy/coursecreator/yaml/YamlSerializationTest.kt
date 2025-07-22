package org.hyperskill.academy.coursecreator.yaml

import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.findTask
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.mapper
import org.hyperskill.academy.learning.yaml.YamlMapper.CURRENT_YAML_VERSION
import org.hyperskill.academy.learning.yaml.YamlTestCase
import org.junit.Test


class YamlSerializationTest : YamlTestCase() {
  @Test
  fun `test remote edu task`() {
    val task = course {
      lesson {
        remoteEduTask {
          taskFile("Test.java")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(
      task, """
    |type: remote_edu
    |files:
    |- name: Test.java
    |  visible: true
    |""".trimMargin()
    )
  }

  @Test
  fun `test remote edu task with check profile`() {
    val checkProfile = "hyperskill_go"
    val task = course {
      lesson {
        remoteEduTask(checkProfile = checkProfile) {
          taskFile("Main.go")
        }
      }
    }.findTask("lesson1", "task1")
    doTest(
      task, """
    |type: remote_edu
    |files:
    |- name: Main.go
    |  visible: true
    |""".trimMargin()
    )
  }

  @Test
  fun `test lesson`() {
    val lesson = course {
      lesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    doTest(
      lesson, """
      |content:
      |- Introduction Task
      |- Advanced Task
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test lesson with custom presentable name`() {
    val lesson = course {
      lesson(customPresentableName = "my new lesson") {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    doTest(
      lesson, """
      |custom_name: ${lesson.customPresentableName}
      |content:
      |- Introduction Task
      |- Advanced Task
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test lesson with content tags`() {
    val lesson = course {
      lesson {
        eduTask("Introduction Task")
        eduTask("Advanced Task")
      }
    }.items[0]
    lesson.contentTags = listOf("kotlin", "cycles")
    @Suppress("DEPRECATION") // using `customPresentableName` here is ok
    doTest(
      lesson, """
      |content:
      |- Introduction Task
      |- Advanced Task
      |tags:
      |- kotlin
      |- cycles
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test hyperskill course`() {
    val course = course(courseProducer = ::HyperskillCourse) {} as HyperskillCourse
    course.apply {
      languageCode = "en"
    }

    doTest(
      course, """
      |type: hyperskill
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |yaml_version: $CURRENT_YAML_VERSION
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test empty lesson`() {
    val lesson = course {
      lesson {
      }
    }.items.first()

    doTest(
      lesson, """
      |{}
      |
    """.trimMargin()
    )
  }

  @Test
  fun `test environment settings`() {
    val course = course {
      lesson("lesson1") {
        eduTask()
      }
    }
    course.environmentSettings += "foo" to "bar"
    doTest(
      course, """
      |type: hyperskill
      |title: Test Course
      |language: English
      |summary: Test Course Description
      |programming_language: Plain text
      |content:
      |- lesson1
      |environment_settings:
      |  foo: bar
      |yaml_version: $CURRENT_YAML_VERSION
      |""".trimMargin()
    )
  }

  private fun doTest(item: StudyItem, expected: String) {
    val actual = mapper().writeValueAsString(item)
    assertEquals(expected, actual)
  }
}