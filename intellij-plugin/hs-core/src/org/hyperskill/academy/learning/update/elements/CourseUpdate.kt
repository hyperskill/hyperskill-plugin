package org.hyperskill.academy.learning.update.elements

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.stepik.hyperskill.update.elements.HyperskillCourseUpdate
import org.hyperskill.academy.learning.update.comparators.EduFileComparator.Companion.areNotEqual
import org.hyperskill.academy.platform.ProgressCompat

abstract class CourseUpdate<T : Course>(
  override val localItem: T,
  override val remoteItem: T
) : StudyItemUpdate<T>(localItem, remoteItem) {
  protected suspend fun baseUpdate(project: Project) {
    localItem.name = remoteItem.name
    localItem.description = remoteItem.description

    if (localItem.additionalFiles areNotEqual remoteItem.additionalFiles) {
      val baseDir = project.courseDir

      writeAction {
        localItem.additionalFiles.forEach { file ->
          baseDir.findChild(file.name)?.delete(this) ?: LOG.warn("${file.name} wasn't found and deleted")
        }
      }

      ProgressCompat.withBlockingIfNeeded {
        remoteItem.additionalFiles.forEach { file ->
          GeneratorUtils.createChildFile(project, baseDir, file.name, file.contents)
        }
      }

      localItem.additionalFiles = remoteItem.additionalFiles
    }

    localItem.sortItems()
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(CourseUpdate::class.java)

    fun <T : Course> get(localCourse: T, remoteCourse: T): CourseUpdate<out Course> = when {
      localCourse is HyperskillCourse && remoteCourse is HyperskillCourse -> HyperskillCourseUpdate(localCourse, remoteCourse)
      else -> error("Unsupported course types: local=${localCourse::class.simpleName}, remote=${remoteCourse::class.simpleName}")
    }
  }
}
