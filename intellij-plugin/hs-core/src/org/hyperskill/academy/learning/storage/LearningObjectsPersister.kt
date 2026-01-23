package org.hyperskill.academy.learning.storage

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.CourseSetListener
import org.hyperskill.academy.learning.courseFormat.Course

class LearningObjectsPersister(private val project: Project) : CourseSetListener {
  override fun courseSet(course: Course) {
    val storageManager = LearningObjectsStorageManager.getInstance(project)

    storageManager.persistAllEduFiles(course)

    course.needWriteYamlText = storageManager.writeTextInYaml
  }
}