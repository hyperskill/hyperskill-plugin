package org.hyperskill.academy.learning.actions.navigate

import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.actions.PreviousTaskAction
import org.hyperskill.academy.learning.checker.CheckUtils
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
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
    val task2 = course.findTask("project", "stage2")

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Mark task1 as solved
      task1.status = CheckStatus.Solved

      // Create user file in task1 (simulating user's work)
      val taskDir = rootDir.findFileByRelativePath("project/task")
        ?: error("Task directory not found")
      createChildData(taskDir.findChild("src")!!, "UserFile.kt", "class UserClass {}")

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

      // Verify user file is preserved and task2 files are added
      val fileTreeAfterNavigation = fileTree {
        dir("project") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 1 template") // Should be preserved from task1
              file("UserFile.kt", "class UserClass {}") // User file should be preserved
              file("NewFile.kt", "// New file in stage 2") // New file from task2 template
            }
            dir("test") {
              file("Tests2.kt", "fun tests2() {}") // Test file from task2
            }
          }
          dir("stage1") { file("task.html") }
          dir("stage2") { file("task.html") }
        }
        file("build.gradle")
        file("settings.gradle")
      }
      fileTreeAfterNavigation.assertEquals(rootDir, myFixture)

      // Navigate back to task1 - user file should still be there
      testAction(PreviousTaskAction.ACTION_ID)

      // Verify we're back to task1 state with user file
      fileTreeWithUserFile.assertEquals(rootDir, myFixture)
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
      val srcDir = taskDir.findChild("src")!!
      createChildData(srcDir, "UserFile1.kt", "class UserClass1 {}")
      createChildData(srcDir, "UserFile2.kt", "class UserClass2 {}")
      createChildData(taskDir, "config.json", "{\"key\": \"value\"}")

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
   * Test that user files are NOT preserved when navigating in regular (non-project) lessons.
   */
  @Test
  fun `test user files not preserved in regular framework lessons`() {
    val course = createRegularFrameworkCourse()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Mark task1 as solved
      task1.status = CheckStatus.Solved

      // Create user file in task1
      val taskDir = rootDir.findFileByRelativePath("lesson1/task")
        ?: error("Task directory not found")
      createChildData(taskDir.findChild("src")!!, "UserFile.kt", "class UserClass {}")

      // Navigate to task2 - user file should NOT be preserved (regular lesson)
      testAction(NextTaskAction.ACTION_ID)

      // Verify user file is NOT preserved (regular propagation rules apply)
      val fileTree = fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 2 template")
              // UserFile.kt should NOT be here
            }
            dir("test") {
              file("Tests2.kt", "fun tests2() {}")
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
   * Test that user files are preserved when navigating backward in project lessons.
   */
  @Test
  fun `test user files preserved when navigating backward in project`() {
    val course = createHyperskillProjectCourse()

    val task1 = course.findTask("project", "stage1")
    val task2 = course.findTask("project", "stage2")

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Mark task1 as solved
      task1.status = CheckStatus.Solved

      // Create user file in task1
      val taskDir = rootDir.findFileByRelativePath("project/task")!!
      createChildData(taskDir.findChild("src")!!, "UserFile.kt", "class UserClass {}")

      // Save snapshot with user file
      frameworkLessonManager.saveSnapshot(task1)

      // Navigate forward to task2
      testAction(NextTaskAction.ACTION_ID)

      // Mark task2 as solved and create different user file
      task2.status = CheckStatus.Solved
      val srcDir = taskDir.findChild("src")!!
      // Delete previous user file to see if backward navigation restores it
      srcDir.findChild("UserFile.kt")?.delete(this)
      createChildData(srcDir, "UserFile2.kt", "class UserClass2 {}")

      // Navigate back to task1
      testAction(PreviousTaskAction.ACTION_ID)

      // Verify original user file from task1 is restored
      val fileTree = fileTree {
        dir("project") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 1 template")
              file("UserFile.kt", "class UserClass {}")
              // UserFile2.kt should NOT be here
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
      fileTree.assertEquals(rootDir, myFixture)
    }
  }

  /**
   * Test that user files are included in snapshots.
   */
  @Test
  fun `test user files saved in snapshots`() {
    val course = createHyperskillProjectCourse()

    val task1 = course.findTask("project", "stage1")
    val lesson = task1.lesson as FrameworkLesson

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Create user file
      val taskDir = rootDir.findFileByRelativePath("project/task")!!
      createChildData(taskDir.findChild("src")!!, "UserFile.kt", "class UserClass {}")

      // Modify template file
      val taskFile = taskDir.findFileByRelativePath("src/Task.kt")!!
      setFileContent(taskFile, "// Modified content", false)

      // Save snapshot
      frameworkLessonManager.saveSnapshot(task1)

      // Get saved state and verify user file is included
      val savedState = frameworkLessonManager.getTaskState(lesson, task1)

      assertNotNull("User file should be in snapshot", savedState["src/UserFile.kt"])
      assertEquals("class UserClass {}", savedState["src/UserFile.kt"])
      assertEquals("// Modified content", savedState["src/Task.kt"])
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
        taskFile("src/Task.kt", "// Stage 1 template")
        taskFile("src/NewFile.kt", "// New file in stage 2")
        taskFile("test/Tests2.kt", "fun tests2() {}")
      }
    }
  }.also { course ->
    // Mark the lesson as the project lesson
    val hyperskillCourse = course as HyperskillCourse
    val lesson = course.lessons.first() as FrameworkLesson
    hyperskillCourse.projectLesson = lesson
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

  private fun createChildData(parent: com.intellij.openapi.vfs.VirtualFile, name: String, content: String) {
    val file = parent.createChildData(this, name)
    setFileContent(file, content, false)
  }

  private fun setFileContent(file: com.intellij.openapi.vfs.VirtualFile, content: String, refreshSync: Boolean) {
    com.intellij.openapi.application.runWriteAction {
      file.setBinaryContent(content.toByteArray())
    }
    if (refreshSync) {
      file.refresh(false, false)
    }
  }
}
