package org.hyperskill.academy.learning.newproject

import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase

abstract class CourseMetadataProcessorTestBase : CourseGenerationTestBase<EmptyProjectSettings>() {
  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  protected fun createCourseWithMetadata(metadata: Map<String, String>) {
    val course = course {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("foo.txt")
        }
      }
    }

    createCourseStructure(course, metadata)
  }
}
