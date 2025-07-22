package org.hyperskill.academy.learning.yaml.format

import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.yaml.errorHandling.loadingError
import org.hyperskill.academy.learning.yaml.errorHandling.unexpectedItemTypeMessage

open class RemoteInfoChangeApplierBase<T : StudyItem> : StudyItemChangeApplier<T>() {
  override fun applyChanges(existingItem: T, deserializedItem: T) {
    existingItem.id = deserializedItem.id
    existingItem.updateDate = deserializedItem.updateDate
  }
}

fun <T : StudyItem> getRemoteChangeApplierForItem(item: T): RemoteInfoChangeApplierBase<T> {
  @Suppress("UNCHECKED_CAST")
  return when (item) {
    is HyperskillCourse -> RemoteHyperskillChangeApplier()
    is RemoteStudyItem -> RemoteInfoChangeApplierBase<T>()
    else -> loadingError(unexpectedItemTypeMessage(item.javaClass.simpleName))
  } as RemoteInfoChangeApplierBase<T>
}
