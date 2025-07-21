package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import org.hamcrest.CoreMatchers.hasItem
import org.hamcrest.CoreMatchers.not
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test

class CourseGenerationTest : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings = EmptyProjectSettings

  @Test
  fun `test do not open invisible files after course creation`() {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("invisible.txt", visible = false)
          taskFile("visible.txt")
        }
      }
    }
    createCourseStructure(course)

    val invisible = findFile("lesson1/task1/invisible.txt")
    val visible = findFile("lesson1/task1/visible.txt")
    val openFiles = FileEditorManager.getInstance(project).openFiles.toList()
    assertThat(openFiles, not(hasItem(invisible)))
    assertThat(openFiles, hasItem(visible))
  }

}
