package org.hyperskill.academy.learning.stepik.hyperskill.update

import org.hyperskill.academy.learning.courseFormat.DescriptionFormat
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.fileTree
import org.hyperskill.academy.learning.update.TaskUpdateTestBase
import org.junit.Test

class HyperskillTaskUpdateTest : TaskUpdateTestBase<HyperskillCourse>() {
  override fun getUpdater(localCourse: HyperskillCourse) = HyperskillCourseUpdaterNew(project, localCourse)

  @Test
  fun `test new task created`() {
    initiateLocalCourse()

    val newEduTask = EduTask("task3").apply {
      id = 3
      index = 3
      taskFiles = linkedMapOf(
        "Task.kt" to TaskFile("src/Task.kt", "fun foo() {}"),
        "Baz.kt" to TaskFile("src/Baz.kt", "fun baz() {}"),
        "Tests.kt" to TaskFile("test/Tests.kt", "fun test3() {}")
      )
      descriptionFormat = DescriptionFormat.HTML
    }
    val newStage = HyperskillStage(3, "", 3)
    val remoteCourse = toRemoteCourse {
      lessons[0].addTask(newEduTask)
      stages = stages + newStage
    }

    updateCourse(remoteCourse)

    assertEquals("Task hasn't been added", 3, findLesson(0).taskList.size)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
        dir("task3") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test new task created in the middle of the lesson`() {
    localCourse = createBasicHyperskillCourse {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1)
        eduTask("task2", stepId = 2)
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
    }
    localCourse.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))

    val newEduTask = EduTask("task3").apply {
      id = 3
      index = 2
      descriptionFormat = DescriptionFormat.HTML
    }
    val newStage = HyperskillStage(3, "", 3)
    val remoteCourse = toRemoteCourse {
      lessons[0].apply {
        taskList[1].index = 3
        addTask(newEduTask)
        sortItems()
      }
      stages = stages + newStage
    }

    updateCourse(remoteCourse)

    val tasks = localCourse.lessons[0].taskList
    assertEquals("Task hasn't been added", 3, tasks.size)
    checkIndices(tasks)
    tasks[0].let { task ->
      assertEquals(1, task.id)
      assertEquals(1, task.index)
      assertEquals("task1", task.name)
      assertEquals("task1", task.presentableName)
    }
    tasks[1].let { task ->
      assertEquals(3, task.id)
      assertEquals(2, task.index)
      assertEquals("task3", task.name)
      assertEquals("task3", task.presentableName)
    }
    tasks[2].let { task ->
      assertEquals(2, task.id)
      assertEquals(3, task.index)
      assertEquals("task2", task.name)
      assertEquals("task2", task.presentableName)
    }

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          file("task.html")
        }
        dir("task3") {
          file("task.html")
        }
        dir("task2") {
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test first task deleted`() {
    initiateLocalCourse()
    val remoteCourse = toRemoteCourse {
      lessons[0].removeTask(taskList[0])
      stages = stages.drop(0)
    }
    updateCourse(remoteCourse)

    assertEquals("Task hasn't been deleted", 1, findLesson(0).taskList.size)
    assertEquals("Task index hasn't been changed", 1, findLesson(0).taskList[0].index)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task2") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test last task deleted`() {
    initiateLocalCourse()

    val remoteCourse = toRemoteCourse {
      lessons[0].removeTask(taskList[1])
      stages.dropLast(1)
    }

    updateCourse(remoteCourse)

    assertTrue("Task hasn't been deleted", findLesson(0).taskList.size == 1)

    val expectedStructure = fileTree {
      dir("lesson1") {
        dir("task1") {
          dir("src") {
            file("Task.kt")
            file("Baz.kt")
          }
          dir("test") {
            file("Tests.kt")
          }
          file("task.html")
        }
      }
      file("build.gradle")
      file("settings.gradle")
    }
    expectedStructure.assertEquals(rootDir)
  }

  @Test
  fun `test task type has been updated from unsupported to supported`() {
    localCourse = createBasicHyperskillCourse {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson1", id = 1) {
          unsupportedTask("task1", stepId = 1)
          eduTask("task2", stepId = 2) {
            taskFile("TaskFile2.kt", "task file 2 text")
          }
        }
      }
    }
    localCourse.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))

    val newEduTask = EduTask("task1").apply {
      id = 1
      index = 1
      taskFiles = linkedMapOf("TaskFile1.kt" to TaskFile("TaskFile1.kt", "task file 1 text"))
    }
    val remoteCourse = toRemoteCourse {
      sections[0].lessons[0].apply {
        removeTask(taskList[0])
        addTask(0, newEduTask)
      }
    }

    updateCourse(remoteCourse)

    assertTrue("UnsupportedTask hasn't been updated to EduTask", findTask(0, 0, 0) is EduTask)
  }

  @Test
  fun `test remoteEduTask and its checkProfile have been updated`() {
    localCourse = createBasicHyperskillCourse {
      section(HYPERSKILL_TOPICS) {
        lesson("lesson1", id = 1) {
          remoteEduTask("task1", stepId = 1, checkProfile = "profile 1")
          remoteEduTask("task2", stepId = 2, checkProfile = "profile 2")
        }
      }
    }
    localCourse.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))

    val newProfile = "profile 2 updated"
    val remoteCourse = toRemoteCourse {
      val task = sections[0].lessons[0].taskList[1] as RemoteEduTask
      task.checkProfile = newProfile
    }

    updateCourse(remoteCourse)

    val newCheckProfile = (findTask(0, 0, 1) as RemoteEduTask).checkProfile
    assertTrue("Sorting options for the SortingTask have not been updated", newCheckProfile == newProfile)
  }

  override fun initiateLocalCourse() {
    localCourse = createBasicHyperskillCourse()
  }
}