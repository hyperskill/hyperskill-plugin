package org.hyperskill.academy.coursecreator.projectView

import com.intellij.ide.projectView.PresentationData
import org.hyperskill.academy.coursecreator.framework.SyncChangesTaskFileState

class CCFilePresentationData : PresentationData() {
  var syncChangesState: SyncChangesTaskFileState? = null

  override fun clear() {
    super.clear()
    syncChangesState = null
  }

  override fun clone(): PresentationData {
    val clone = super.clone() as CCFilePresentationData
    clone.syncChangesState = syncChangesState
    return clone
  }

  override fun copyFrom(from: PresentationData) {
    super.copyFrom(from)
    if (from is CCFilePresentationData) {
      syncChangesState = from.syncChangesState
    }
  }
}