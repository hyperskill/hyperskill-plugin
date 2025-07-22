package org.hyperskill.academy.learning.update

import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.submissions.isSignificantlyAfter

sealed interface ItemUpdater<T : StudyItem> {
  fun T.isOutdated(remoteItem: T): Boolean
}

interface HyperskillItemUpdater<T : StudyItem> : ItemUpdater<T> {
  override fun T.isOutdated(remoteItem: T): Boolean = remoteItem.updateDate.isSignificantlyAfter(updateDate)
}
