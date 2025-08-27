package org.hyperskill.academy.learning.yaml.format

import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ItemContainer
import org.hyperskill.academy.learning.courseFormat.StudyItem

/**
 * Placeholder for any StudyItem, should be filled with actual content later
 */
class TitledStudyItem(title: String) : StudyItem(title) {

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    throw NotImplementedError()
  }

  override val course: Course
    get() = throw NotImplementedError()
  override val itemType: String
    get() = throw NotImplementedError()
}