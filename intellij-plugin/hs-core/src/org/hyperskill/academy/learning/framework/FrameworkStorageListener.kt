package org.hyperskill.academy.learning.framework

import com.intellij.util.messages.Topic

/**
 * Listener for Framework Storage changes.
 * Used to notify UI components when commits are created or HEAD changes.
 */
interface FrameworkStorageListener {

  /**
   * Called when a new snapshot is saved to storage.
   * @param refId The ref ID that was updated
   * @param commitHash The hash of the new commit
   */
  fun snapshotSaved(refId: Int, commitHash: String)

  /**
   * Called when HEAD is updated to point to a different ref.
   * @param refId The new HEAD ref ID, or -1 if HEAD was cleared
   */
  fun headUpdated(refId: Int)

  companion object {
    @JvmField
    val TOPIC: Topic<FrameworkStorageListener> = Topic.create(
      "Framework Storage Changes",
      FrameworkStorageListener::class.java
    )
  }
}
