package org.hyperskill.academy.learning.format.yaml

import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.yaml.YamlTestCase
import org.hyperskill.academy.learning.yaml.format.getChangeApplierForItem
import org.junit.Test

class StudentChangeApplierTest : YamlTestCase() {

  @Test
  fun `test edu task cc fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        eduTask("task1")
      }
    }.lessons.first().taskList.first()
    val deserializedItem = EduTask("task1")
    deserializedItem.customPresentableName = "custom name"
    deserializedItem.feedbackLink = "test"

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.record, existingItem.record)
    @Suppress("DEPRECATION")
    assertEquals(deserializedItem.customPresentableName, existingItem.customPresentableName)
    assertEquals(deserializedItem.feedbackLink, existingItem.feedbackLink)
  }

  @Test
  fun `test edu task student fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        eduTask("task1")
      }
    }.lessons.first().taskList.first()
    val deserializedItem = EduTask("task1")
    deserializedItem.record = 1

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.record, existingItem.record)
  }

  @Test
  fun `test task file fields applied`() {
    val existingItem = courseWithFiles {
      lesson {
        eduTask("task1") {
          taskFile("taskFile.txt", "code")
        }
      }
    }.lessons.first().taskList.first()
    val deserializedItem = EduTask("task1")
    deserializedItem.customPresentableName = "custom name"
    deserializedItem.feedbackLink = "test"
    deserializedItem.taskFiles = linkedMapOf("taskFile.txt" to TaskFile("taskFile.txt", "new code"))

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals(deserializedItem.taskFiles.values.first().text, existingItem.taskFiles.values.first().text)
  }

  @Test
  fun `test remote edu task keeps existing check profile when deserialized value is empty`() {
    val existingItem = courseWithFiles {
      lesson {
        remoteEduTask("task1", checkProfile = "hyperskill_go")
      }
    }.lessons.first().taskList.first() as RemoteEduTask
    val deserializedItem = RemoteEduTask().apply {
      name = "task1"
      checkProfile = ""
    }

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals("hyperskill_go", existingItem.checkProfile)
  }

  @Test
  fun `test remote edu task updates check profile when deserialized value is non empty`() {
    val existingItem = courseWithFiles {
      lesson {
        remoteEduTask("task1", checkProfile = "hyperskill_go")
      }
    }.lessons.first().taskList.first() as RemoteEduTask
    val deserializedItem = RemoteEduTask().apply {
      name = "task1"
      checkProfile = "hyperskill_kotlin"
    }

    getChangeApplierForItem(project, existingItem).applyChanges(existingItem, deserializedItem)

    assertEquals("hyperskill_kotlin", existingItem.checkProfile)
  }
}
