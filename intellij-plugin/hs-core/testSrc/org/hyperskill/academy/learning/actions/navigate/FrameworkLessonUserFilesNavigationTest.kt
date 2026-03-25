package org.hyperskill.academy.learning.actions.navigate

import com.intellij.openapi.application.runWriteAction
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.actions.PreviousTaskAction
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.createChildFile
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.junit.Test

/**
 * Tests for ALT-10993: Framework lesson navigation should preserve user-created files.
 *
 * The issue: When navigating between solved tasks in a project lesson, user-created files
 * (files not in the template) were being lost because the navigation logic only read
 * template files from disk, not all files.
 *
 * The fix: Use getAllFilesFromTaskDir() to read ALL files from disk, including user-created ones.
 * Additionally, when navigating forward from a solved task in the same project lesson,
 * preserve user files by treating it like a first visit (add only new template files).
 */
class FrameworkLessonUserFilesNavigationTest : NavigationTestBase() {

  /**
   * Test that user-created files are preserved when navigating forward through solved tasks
   * in a project lesson.
   */
  @Test
  fun `test user-created files preserved in solved project tasks`() {
    val course = createHyperskillProjectCourse()

    val task1 = course.findTask("project", "stage1")

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Mark task1 as solved
      task1.status = CheckStatus.Solved

      // Create user file in task1 (simulating user's work)
      run {
        val taskDir = rootDir.findFileByRelativePath("project/task")
          ?: error("Task directory not found")
        createChildFile(project, taskDir, "src/UserFile.kt", InMemoryTextualContents("class UserClass {}"))
      }

      // Verify user file exists
      val fileTreeWithUserFile = fileTree {
        dir("project") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 1 template")
              file("UserFile.kt", "class UserClass {}")
            }
            dir("test") {
              file("Tests1.kt", "fun tests1() {}")
            }
          }
          dir("stage1") { file("task.html") }
          dir("stage2") { file("task.html") }
        }
        file("build.gradle")
        file("settings.gradle")
      }
      fileTreeWithUserFile.assertEquals(rootDir, myFixture)

      // Navigate to task2 (forward navigation in solved project task)
      testAction(NextTaskAction.ACTION_ID)

      // Verify user file is preserved (main goal of this test)
      run {
        val taskDir = rootDir.findFileByRelativePath("project/task")!!
        val userFile = taskDir.findFileByRelativePath("src/UserFile.kt")
        assertNotNull("User file should be preserved after navigation to task2", userFile)

        val userFileContent = runWriteAction {
          userFile!!.contentsToByteArray().decodeToString()
        }
        assertEquals("class UserClass {}", userFileContent)
      }
    }
  }

  /**
   * Test that getAllFilesFromTaskDir captures all files including user-created ones.
   */
  @Test
  fun `test getAllFilesFromTaskDir captures user-created files`() {
    val course = createHyperskillProjectCourse()

    val task1 = course.findTask("project", "stage1")
    val lesson = task1.lesson as FrameworkLesson

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Create multiple user files in different directories
      val taskDir = rootDir.findFileByRelativePath("project/task")
        ?: error("Task directory not found")
      createChildFile(project, taskDir, "src/UserFile1.kt", InMemoryTextualContents("class UserClass1 {}"))
      createChildFile(project, taskDir, "src/UserFile2.kt", InMemoryTextualContents("class UserClass2 {}"))
      createChildFile(project, taskDir, "config.json", InMemoryTextualContents("{\"key\": \"value\"}"))

      // Get task state - should include all files
      val taskState = frameworkLessonManager.getTaskState(lesson, task1)

      // Verify all files are captured
      assertNotNull("Template file should be captured", taskState["src/Task.kt"])
      assertNotNull("User file 1 should be captured", taskState["src/UserFile1.kt"])
      assertNotNull("User file 2 should be captured", taskState["src/UserFile2.kt"])
      assertNotNull("Config file should be captured", taskState["config.json"])
      assertEquals("class UserClass1 {}", taskState["src/UserFile1.kt"])
      assertEquals("class UserClass2 {}", taskState["src/UserFile2.kt"])
      assertEquals("{\"key\": \"value\"}", taskState["config.json"])
    }
  }

  /**
   * Test that user files ARE propagated in regular (non-project) lessons when navigating forward.
   * This is the normal propagation behavior - ALL files (including user-created) are propagated forward.
   */
  @Test
  fun `test user files propagated in regular framework lessons`() {
    val course = createRegularFrameworkCourse()

    val task1 = course.findTask("lesson1", "task1")

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Create user file in task1
      val taskDir = rootDir.findFileByRelativePath("lesson1/task")
        ?: error("Task directory not found")
      createChildFile(project, taskDir, "src/UserFile.kt", InMemoryTextualContents("class UserClass {}"))

      // Navigate to task2 - ALL files SHOULD be propagated (normal forward propagation)
      testAction(NextTaskAction.ACTION_ID)

      // Verify user file IS propagated to task2 (normal forward propagation)
      // Note: Task.kt content is also propagated from task1 (not replaced with task2 template)
      val fileTree = fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 1 template") // Propagated from task1
              file("UserFile.kt", "class UserClass {}") // User file is propagated
            }
            dir("test") {
              file("Tests2.kt", "fun tests2() {}") // Test file from task2
            }
          }
          dir("task1") { file("task.html") }
          dir("task2") { file("task.html") }
        }
        file("build.gradle")
        file("settings.gradle")
      }
      fileTree.assertEquals(rootDir, myFixture)
    }
  }

  /**
   * Test that snapshots correctly save and restore user files during navigation.
   */
  @Test
  fun `test user files restored from snapshots`() {
    val course = createHyperskillProjectCourse()

    val task1 = course.findTask("project", "stage1")

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Mark task1 as solved
      task1.status = CheckStatus.Solved

      // Create user file in task1
      val taskDir = rootDir.findFileByRelativePath("project/task")!!
      createChildFile(project, taskDir, "src/UserFile.kt", InMemoryTextualContents("class UserClass {}"))

      // Verify user file exists before navigation
      val fileTreeBefore = fileTree {
        dir("project") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 1 template")
              file("UserFile.kt", "class UserClass {}")
            }
            dir("test") {
              file("Tests1.kt", "fun tests1() {}")
            }
          }
          dir("stage1") { file("task.html") }
          dir("stage2") { file("task.html") }
        }
        file("build.gradle")
        file("settings.gradle")
      }
      fileTreeBefore.assertEquals(rootDir, myFixture)

      // Navigate forward to task2
      testAction(NextTaskAction.ACTION_ID)

      // Navigate back to task1
      testAction(PreviousTaskAction.ACTION_ID)

      // Verify original user file from task1 is restored
      val fileTreeAfter = fileTree {
        dir("project") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 1 template")
              file("UserFile.kt", "class UserClass {}")
            }
            dir("test") {
              file("Tests1.kt", "fun tests1() {}")
            }
          }
          dir("stage1") { file("task.html") }
          dir("stage2") { file("task.html") }
        }
        file("build.gradle")
        file("settings.gradle")
      }
      fileTreeAfter.assertEquals(rootDir, myFixture)
    }
  }

  /**
   * Test that getTaskState captures user files from disk.
   */
  @Test
  fun `test getTaskState includes user files`() {
    val course = createHyperskillProjectCourse()

    val task1 = course.findTask("project", "stage1")
    val lesson = task1.lesson as FrameworkLesson

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Create user file
      val taskDir = rootDir.findFileByRelativePath("project/task")!!
      createChildFile(project, taskDir, "src/UserFile.kt", InMemoryTextualContents("class UserClass {}"))

      // Modify template file
      val taskFile = taskDir.findFileByRelativePath("src/Task.kt")!!
      runWriteAction {
        taskFile.setBinaryContent("// Modified content".toByteArray())
      }

      // Get task state and verify user file is included
      val taskState = frameworkLessonManager.getTaskState(lesson, task1)

      assertNotNull("User file should be captured", taskState["src/UserFile.kt"])
      assertEquals("class UserClass {}", taskState["src/UserFile.kt"])
      assertEquals("// Modified content", taskState["src/Task.kt"])
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

  private fun createRegularFrameworkCourse(): Course = courseWithFiles(
    language = FakeGradleBasedLanguage
  ) {
    frameworkLesson("lesson1", isTemplateBased = false) {
      eduTask("task1", stepId = 3001) {
        taskFile("src/Task.kt", "// Stage 1 template")
        taskFile("test/Tests1.kt", "fun tests1() {}")
      }
      eduTask("task2", stepId = 3002) {
        taskFile("src/Task.kt", "// Stage 2 template")
        taskFile("test/Tests2.kt", "fun tests2() {}")
      }
    }
  }

}
