package org.hyperskill.academy.coursecreator.yaml

import org.hyperskill.academy.coursecreator.AdditionalFilesUtils.collectAdditionalFiles
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.junit.Test
import kotlin.test.assertContentEquals

class AdditionalFilesInsideYamlTest : EduTestCase() {

  @Test
  fun `collecting additional files does not need the course object`() {
    courseWithFiles(createYamlConfigs = true) {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("Task.kt")
        }
      }
      additionalFile("file1.txt")
      additionalFile("lesson1/file2.txt")
      additionalFile("lesson1/task1/file3.txt") // not an additional file, because is inside the folder of the "task1" task
    }

    val course = project.course!!
    StudyTaskManager.getInstance(project).course = HyperskillCourse() // this course has no tasks

    val additionalFiles = collectAdditionalFiles(course.configurator, project, detectTaskFoldersByContents = true)
      .sortedBy { it.name }

    assertContentEquals(listOf("file1.txt", "lesson1/file2.txt"), additionalFiles.map { it.name })
  }

}