package org.hyperskill.academy.learning.actions.navigate

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.createChildFile
import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.actions.PreviousTaskAction
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.findTask
import org.hyperskill.academy.learning.testAction
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.junit.Test
import org.junit.Assert.*

class FrameworkLessonUserFilesPropagationTest : NavigationTestBase() {

  @Test
  fun `test user-created files propagated and registered in next stage`() {
    val course = createHyperskillProjectCourse()
    val task1 = course.findTask("project", "stage1")
    val task2 = course.findTask("project", "stage2")

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")
      task1.status = CheckStatus.Solved

      // Create user file in task1
      val taskDir = rootDir.findFileByRelativePath("project/task") ?: error("Task directory not found")
      runWriteAction {
        createChildFile(project, taskDir, "src/UserFile.kt", InMemoryTextualContents("class UserClass {}"))
      }

      // Navigate to task2
      testAction(NextTaskAction.ACTION_ID)

      // 1. Verify user file is on disk
      val userFile = taskDir.findFileByRelativePath("src/UserFile.kt")
      assertNotNull("User file should be on disk after navigation to task2", userFile)

      // 2. Verify user file is registered in task2's taskFiles
      val taskFile = task2.getTaskFile("src/UserFile.kt")
      assertNotNull("User file should be registered in task2 taskFiles", taskFile)
      assertTrue("User file should be marked as learner created", taskFile!!.isLearnerCreated)
    }
  }

  @Test
  fun `test excluded files are NOT captured in snapshot`() {
    val course = createHyperskillProjectCourse()
    val task1 = course.findTask("project", "stage1")

    withVirtualFileListener(course) { 
      task1.openTaskFileInEditor("src/Task.kt")
      task1.status = CheckStatus.Solved

      // Create excluded directory and file
      val taskDir = rootDir.findFileByRelativePath("project/task") ?: error("Task directory not found")
      runWriteAction {
        val buildDir = taskDir.createChildDirectory(this, ".build")
        buildDir.createChildData(this, "some_generated_file.txt")
      }

      // Navigate to task2 (this triggers saveCurrentTaskSnapshot for task1)
      testAction(NextTaskAction.ACTION_ID)

      // Verify that 'build/' is NOT in task1's snapshot in storage
      val manager = FrameworkLessonManager.getInstance(project)
      val task1State = manager.getTaskState(task1.lesson as FrameworkLesson, task1)
      assertFalse("Excluded file should not be in task state", task1State.containsKey(".build/some_generated_file.txt"))
    }
  }

  @Test
  fun `test auto-save persists deletion of all propagatable files to snapshot`() {
    val course = createHyperskillProjectCourse()
    val task1 = course.findTask("project", "stage1")
    val task2 = course.findTask("project", "stage2")

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Navigate forward to task2 to establish a snapshot baseline that contains src/Task.kt.
      testAction(NextTaskAction.ACTION_ID)

      val taskDir = rootDir.findFileByRelativePath("project/task") ?: error("Task directory not found")
      assertNotNull("Baseline: src/Task.kt should exist on disk in task2", taskDir.findFileByRelativePath("src/Task.kt"))

      // Delete the only propagatable file from disk, then trigger auto-save (Ctrl+S equivalent).
      runWriteAction {
        taskDir.findFileByRelativePath("src/Task.kt")?.delete(this)
      }
      FileDocumentManager.getInstance().saveAllDocuments()

      // Navigate BACK to task1. Backward navigation does not overwrite task2's snapshot,
      // so any state left there by auto-save is preserved for inspection.
      testAction(PreviousTaskAction.ACTION_ID)

      // With the bug, saveCurrentTaskSnapshot bailed out when propagatableFiles was empty,
      // leaving src/Task.kt in task2's snapshot — which would resurrect the file on next visit.
      val manager = FrameworkLessonManager.getInstance(project)
      val task2State = manager.getTaskState(task2.lesson as FrameworkLesson, task2)
      assertFalse(
        "Snapshot must reflect deletion: src/Task.kt should not be in task2 state after auto-save",
        task2State.containsKey("src/Task.kt")
      )
    }
  }

  private fun createHyperskillProjectCourse(): Course = courseWithFiles(
    language = FakeGradleBasedLanguage,
    courseProducer = ::HyperskillCourse
  ) {
    frameworkLesson("project", isTemplateBased = false) {
      eduTask("stage1", stepId = 2001) {
        taskFile("src/Task.kt", "// Stage 1 template")
        taskFile("test/Tests1.kt", "fun tests1() {}")
      }
      eduTask("stage2", stepId = 2002) {
        taskFile("src/Task.kt", "// Stage 2 template")
        taskFile("test/Tests2.kt", "fun tests2() {}")
      }
    }
  }
}
