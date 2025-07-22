package org.hyperskill.academy.learning.update.elements

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.StudyItem

/**
 * This base class is used to store updating information of the `StudyItem` object
 */
sealed class StudyItemUpdate<out T : StudyItem>(open val localItem: T?, open val remoteItem: T?) {
  abstract suspend fun update(project: Project)
}