package org.hyperskill.academy.learning.actions.rename

import com.intellij.testFramework.LightPlatformTestCase
import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.courseFormat.DescriptionFormat
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.findTask
import org.hyperskill.academy.learning.testAction
import org.junit.Test

class RenameTest : RenameTestBase() {

  @Test
  fun `test forbid section renaming in student mode`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }

    doRenameAction(course, "section1", "section2", shouldBeInvoked = false)
    assertEquals(1, course.items.size)
    assertNull(course.getSection("section2"))
    assertNotNull(course.getSection("section1"))
  }

  @Test
  fun `test forbid lesson renaming in section in student mode`() {
    val course = courseWithFiles {
      section {
        lesson {
          eduTask {
            taskFile("taskFile1.txt")
          }
        }
      }
    }

    doRenameAction(course, "section1/lesson1", "lesson2", shouldBeInvoked = false)
    assertEquals(1, course.items.size)
    val section = course.getSection("section1")!!
    assertNotNull(section)
    assertNotNull(section.getLesson("lesson1"))
    assertNull(section.getLesson("lesson2"))
  }

  @Test
  fun `test forbid lesson renaming in course in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    doRenameAction(course, "lesson1", "lesson2", shouldBeInvoked = false)
    assertEquals(1, course.items.size)
    assertNotNull(course.getLesson("lesson1"))
    assertNull(course.getLesson("lesson2"))
  }

  @Test
  fun `test forbid task renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    doRenameAction(course, "lesson1/task1", "task2", shouldBeInvoked = false)
    assertEquals(1, course.items.size)
    val lesson = course.getLesson("lesson1")!!
    assertNotNull(lesson)
    assertNotNull(lesson.getTask("task1"))
    assertNull(lesson.getTask("task2"))
  }

  @Test
  fun `test forbid task description file renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }

    val taskHtml = DescriptionFormat.HTML.fileName
    val taskMd = DescriptionFormat.MD.fileName

    doRenameAction(
      course,
      "lesson1/task1/$taskHtml",
      taskMd,
      shouldBeInvoked = false
    )
    assertEquals(DescriptionFormat.HTML, course.lessons[0].taskList[0].descriptionFormat)
    assertNull(findDescriptionFile(taskMd))
    assertNotNull(findDescriptionFile(taskHtml))
  }

  @Test
  fun `test rename task file renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
    }
    // When there isn't any rename handler for file, rename action uses default one.
    // And default rename handler has special code for unit tests not to show rename dialog at all
    doRenameAction(course, "lesson1/task1/taskFile1.txt", "taskFile2.txt", shouldBeShown = false)
    val task = course.findTask("lesson1", "task1")
    assertNull(task.getTaskFile("taskFile1.txt"))
    assertNotNull(task.getTaskFile("taskFile2.txt"))
  }

  @Test
  fun `test rename student created task file in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }

    }
    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, findFile("lesson1/task1"), "taskFile2.txt", "")
    }
    doRenameAction(course, "lesson1/task1/taskFile2.txt", "taskFile3.txt", shouldBeShown = false)
    val task = course.findTask("lesson1", "task1")
    assertNull(task.getTaskFile("taskFile2.txt"))
    assertNotNull(task.getTaskFile("taskFile3.txt"))
  }

  @Test
  fun `test forbid course additional file renaming in student mode`() {
    val course = courseWithFiles {
      lesson {
        eduTask {
          taskFile("taskFile1.txt")
        }
      }
      additionalFile("additionalFile1.txt")
    }
    doRenameAction(course, "additionalFile1.txt", "additionalFile2.txt", shouldBeInvoked = false)
    assertNull(LightPlatformTestCase.getSourceRoot().findFileByRelativePath("additionalFile2.txt"))
    assertNull(course.additionalFiles.find { it.name == "additionalFile2.txt" })
    assertNotNull(LightPlatformTestCase.getSourceRoot().findFileByRelativePath("additionalFile1.txt"))
    assertNotNull(course.additionalFiles.find { it.name == "additionalFile1.txt" })
  }

  @Test
  fun `test rename student created task file in student mode in hyperskill course`() {
    val course = courseWithFiles(courseProducer = ::HyperskillCourse) {
      frameworkLesson {
        eduTask("task1") {
          taskFile("taskFile1.txt")
        }
        eduTask("task2") {
          taskFile("taskFile1.txt")
        }
      }
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject()
    course.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2, true))

    withVirtualFileListener(course) {
      GeneratorUtils.createTextChildFile(project, findFile("lesson1/task"), "taskFile2.txt", "")
      val task1 = course.findTask("lesson1", "task1")
      task1.openTaskFileInEditor("taskFile2.txt")
      testAction(NextTaskAction.ACTION_ID)
    }

    doRenameAction(course, "lesson1/task/taskFile2.txt", "taskFile3.txt", shouldBeShown = false)

    val task2 = course.findTask("lesson1", "task2")
    assertNull(task2.getTaskFile("taskFile2.txt"))
    assertNotNull(task2.getTaskFile("taskFile3.txt"))
  }
}
