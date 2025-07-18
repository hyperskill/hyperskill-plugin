package com.jetbrains.edu.coursecreator

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.Function
import com.jetbrains.edu.learning.courseFormat.StudyItem

object CCUtils {
  const val GENERATED_FILES_FOLDER = ".coursecreator"
  const val DEFAULT_PLACEHOLDER_TEXT = "type here"
  private const val IS_LOCAL_COURSE: String = "Edu.IsLocalCourse"

  private val INDEX_COMPARATOR = Comparator.comparingInt(StudyItem::index)

  var Project.isLocalCourse: Boolean
    get() = PropertiesComponent.getInstance(this).getBoolean(IS_LOCAL_COURSE)
    set(value) = PropertiesComponent.getInstance(this).setValue(IS_LOCAL_COURSE, value)

  /**
   * This method decreases index and updates directory names of
   * all tasks/lessons that have higher index than specified object
   *
   * @param dirs         directories that are used to get tasks/lessons
   * @param getStudyItem function that is used to get task/lesson from VirtualFile. This function can return null
   * @param threshold    index is used as threshold
   */
  fun updateHigherElements(
    dirs: Array<VirtualFile>,
    getStudyItem: Function<VirtualFile, out StudyItem?>,
    threshold: Int,
    delta: Int
  ) {
    val itemsToUpdate = dirs
      .mapNotNull { getStudyItem.`fun`(it) }
      .filter { it.index > threshold }
      .sortedWith { item1, item2 ->
        // if we delete some dir we should start increasing numbers in dir names from the end
        -delta * INDEX_COMPARATOR.compare(item1, item2)
      }

    for (item in itemsToUpdate) {
      val newIndex = item.index + delta
      item.index = newIndex
    }
  }

}
