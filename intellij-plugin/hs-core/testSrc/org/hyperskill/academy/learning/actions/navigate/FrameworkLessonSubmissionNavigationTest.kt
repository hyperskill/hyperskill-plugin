package org.hyperskill.academy.learning.actions.navigate

import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.actions.NextTaskAction
import org.hyperskill.academy.learning.actions.PreviousTaskAction
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * Tests for ALT-10961: Framework lesson content should be preserved after navigation.
 *
 * The issue was that when submissions are loaded and saved via [FrameworkLessonManager.saveExternalChanges],
 * navigating through stages would overwrite the saved submissions with incorrect content.
 *
 * The root cause: In framework lessons, all stages share the same task directory on disk.
 * When navigating, [FrameworkLessonManager.applyTargetTaskChanges] used [Task.allFiles] which reads
 * from disk (same for all stages), not the original template. This caused:
 * 1. getUserChangesFromFiles() to detect no changes (comparing disk to disk)
 * 2. Correct submission data from saveExternalChanges() to be overwritten with empty/wrong diff
 */
class FrameworkLessonSubmissionNavigationTest : NavigationTestBase() {

  /**
   * Test that saveExternalChanges saves different content for each stage
   * and navigation restores the correct content for each stage.
   */
  @Test
  fun `test saveExternalChanges content is preserved after navigation`() {
    val course = createFrameworkCourseWithDifferentContent()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")
    val task3 = course.findTask("lesson1", "task3")

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

    // ALT-10961: Clear the template cache to simulate the real scenario where
    // project is loaded from YAML (cache is empty)
    frameworkLessonManager.cleanUpState()
    frameworkLessonManager.restoreState()

    // Simulate loading submissions by calling saveExternalChanges with different content for each stage
    val submission1 = mapOf("src/Task.kt" to "// Stage 1 solution\nfun stage1() {}")
    val submission2 = mapOf("src/Task.kt" to "// Stage 2 solution\nfun stage2() { println(\"hello\") }")
    val submission3 = mapOf("src/Task.kt" to "// Stage 3 solution\nfun stage3() { val x = 42 }")

    frameworkLessonManager.saveExternalChanges(task1, submission1)
    frameworkLessonManager.saveExternalChanges(task2, submission2)
    frameworkLessonManager.saveExternalChanges(task3, submission3)

    // Navigate from task1 to task3
    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")
      testAction(NextTaskAction.ACTION_ID)

      // Verify task2 content
      val fileTree2 = fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 2 solution\nfun stage2() { println(\"hello\") }")
            }
            dir("test") {
              file("Tests2.kt", "fun tests2() {}")
            }
          }
          dir("task1") { file("task.html") }
          dir("task2") { file("task.html") }
          dir("task3") { file("task.html") }
        }
        file("build.gradle")
        file("settings.gradle")
      }
      fileTree2.assertEquals(rootDir, myFixture)

      testAction(NextTaskAction.ACTION_ID)

      // Verify task3 content
      val fileTree3 = fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 3 solution\nfun stage3() { val x = 42 }")
            }
            dir("test") {
              file("Tests3.kt", "fun tests3() {}")
            }
          }
          dir("task1") { file("task.html") }
          dir("task2") { file("task.html") }
          dir("task3") { file("task.html") }
        }
        file("build.gradle")
        file("settings.gradle")
      }
      fileTree3.assertEquals(rootDir, myFixture)

      // Navigate back to task1
      testAction(PreviousTaskAction.ACTION_ID)
      testAction(PreviousTaskAction.ACTION_ID)

      // Verify task1 content is preserved
      val fileTree1 = fileTree {
        dir("lesson1") {
          dir("task") {
            dir("src") {
              file("Task.kt", "// Stage 1 solution\nfun stage1() {}")
            }
            dir("test") {
              file("Tests1.kt", "fun tests1() {}")
            }
          }
          dir("task1") { file("task.html") }
          dir("task2") { file("task.html") }
          dir("task3") { file("task.html") }
        }
        file("build.gradle")
        file("settings.gradle")
      }
      fileTree1.assertEquals(rootDir, myFixture)
    }
  }

  /**
   * Test that navigating back and forth multiple times preserves the correct content.
   *
   * ALT-10961 BUG SCENARIO:
   * 1. saveExternalChanges saves correct submission for each stage
   * 2. User navigates from Stage 1 to Stage 2
   * 3. BUG: Disk content doesn't change - still shows Stage 1 content
   * 4. Expected: Disk should show Stage 2 content after navigation
   */
  @Test
  fun `test multiple navigation preserves submission content`() {
    val course = createFrameworkCourseWithDifferentContent()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")
    val task3 = course.findTask("lesson1", "task3")

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

    // ALT-10961: Clear the template cache to simulate loading from YAML
    frameworkLessonManager.cleanUpState()
    frameworkLessonManager.restoreState()

    // Save DIFFERENT content for each stage - this simulates loading submissions from API
    val submission1 = mapOf("src/Task.kt" to "STAGE_1_CONTENT")
    val submission2 = mapOf("src/Task.kt" to "STAGE_2_CONTENT_LONGER")  // Different length!
    val submission3 = mapOf("src/Task.kt" to "STAGE_3_CONTENT_EVEN_LONGER")  // Different length!

    frameworkLessonManager.saveExternalChanges(task1, submission1)
    frameworkLessonManager.saveExternalChanges(task2, submission2)
    frameworkLessonManager.saveExternalChanges(task3, submission3)

    withVirtualFileListener(course) {
      task1.openTaskFileInEditor("src/Task.kt")

      // Verify initial disk content (should be template or stage 1 content)
      val initialContent = findFileInTaskDir("src/Task.kt").contentsToByteArray().decodeToString()

      // Navigate to task2 - disk content MUST change
      testAction(NextTaskAction.ACTION_ID)
      val task2Content = findFileInTaskDir("src/Task.kt").contentsToByteArray().decodeToString()

      // ALT-10961: THIS IS THE KEY ASSERTION - content must change after navigation!
      assertNotEquals(
        "Disk content should change after navigation from Stage 1 to Stage 2",
        initialContent,
        task2Content
      )
      assertEquals("STAGE_2_CONTENT_LONGER", task2Content)

      // Navigate to task3 - disk content MUST change again
      testAction(NextTaskAction.ACTION_ID)
      val task3Content = findFileInTaskDir("src/Task.kt").contentsToByteArray().decodeToString()
      assertNotEquals(
        "Disk content should change after navigation from Stage 2 to Stage 3",
        task2Content,
        task3Content
      )
      assertEquals("STAGE_3_CONTENT_EVEN_LONGER", task3Content)

      // Navigate back to task1 - should return to stage 1 content
      testAction(PreviousTaskAction.ACTION_ID) // -> task2
      testAction(PreviousTaskAction.ACTION_ID) // -> task1
      val backToTask1Content = findFileInTaskDir("src/Task.kt").contentsToByteArray().decodeToString()
      assertEquals("STAGE_1_CONTENT", backToTask1Content)
    }
  }

  /**
   * Test that getTaskState returns the correct content after saveExternalChanges.
   */
  @Test
  fun `test getTaskState returns saved external changes`() {
    val course = createFrameworkCourseWithDifferentContent()

    val task1 = course.findTask("lesson1", "task1")
    val task2 = course.findTask("lesson1", "task2")
    val task3 = course.findTask("lesson1", "task3")
    val lesson = task1.lesson as FrameworkLesson

    val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

    // ALT-10961: Clear the template cache to simulate loading from YAML
    frameworkLessonManager.cleanUpState()
    frameworkLessonManager.restoreState()

    val submission1 = mapOf("src/Task.kt" to "CONTENT_1")
    val submission2 = mapOf("src/Task.kt" to "CONTENT_2_LONGER")
    val submission3 = mapOf("src/Task.kt" to "CONTENT_3_EVEN_LONGER_TEXT")

    frameworkLessonManager.saveExternalChanges(task1, submission1)
    frameworkLessonManager.saveExternalChanges(task2, submission2)
    frameworkLessonManager.saveExternalChanges(task3, submission3)

    // Verify getTaskState returns the saved content
    val state1 = frameworkLessonManager.getTaskState(lesson, task1)
    val state2 = frameworkLessonManager.getTaskState(lesson, task2)
    val state3 = frameworkLessonManager.getTaskState(lesson, task3)

    assertEquals("CONTENT_1", state1["src/Task.kt"])
    assertEquals("CONTENT_2_LONGER", state2["src/Task.kt"])
    assertEquals("CONTENT_3_EVEN_LONGER_TEXT", state3["src/Task.kt"])
  }

  private fun findFileInTaskDir(relativePath: String) =
    rootDir.findFileByRelativePath("lesson1/task/$relativePath")
      ?: error("File not found: lesson1/task/$relativePath")

  private fun createFrameworkCourseWithDifferentContent(): Course = courseWithFiles(
    language = FakeGradleBasedLanguage
  ) {
    frameworkLesson("lesson1", isTemplateBased = false) {
      eduTask("task1", stepId = 1001) {
        taskFile("src/Task.kt", "fun template() {}")
        taskFile("test/Tests1.kt", "fun tests1() {}")
      }
      eduTask("task2", stepId = 1002) {
        taskFile("src/Task.kt", "fun template() {}")
        taskFile("test/Tests2.kt", "fun tests2() {}")
      }
      eduTask("task3", stepId = 1003) {
        taskFile("src/Task.kt", "fun template() {}")
        taskFile("test/Tests3.kt", "fun tests3() {}")
      }
    }
  }
}
