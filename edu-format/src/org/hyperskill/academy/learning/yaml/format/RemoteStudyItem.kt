package org.hyperskill.academy.learning.yaml.format

import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ItemContainer
import org.hyperskill.academy.learning.courseFormat.StudyItem

/**
 * Placeholder to deserialize [StudyItem] with remote info. Remote info is applied to
 * existing StudyItem by [org.hyperskill.academy.learning.yaml.format.RemoteInfoChangeApplierBase]
 */
class RemoteStudyItem : StudyItem() {

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    throw NotImplementedError()
  }

  override val course: Course
    get() = throw NotImplementedError()
  override val itemType: String
    get() = throw NotImplementedError()

}