package com.jetbrains.edu.learning.courseGeneration

import com.intellij.openapi.fileEditor.FileEditorManager
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.newproject.EmptyProjectSettings
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
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


  @Test
  fun `test course preview not added to course storage`() {
    val coursePreview = (course {
      lesson("lesson1") {
        eduTask("task1") {
        }
      }
    } as HyperskillCourse)

    createCourseStructure(coursePreview)

    assertFalse(
      "Course `${coursePreview.name}` shouldn't be added to course storage",
      CoursesStorage.getInstance().hasCourse(coursePreview)
    )
  }
}
