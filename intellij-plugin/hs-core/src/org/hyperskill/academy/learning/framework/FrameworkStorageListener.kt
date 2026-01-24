package org.hyperskill.academy.learning.framework

import com.intellij.util.messages.Topic

/**
 * Listener for Framework Storage changes.
 * Used to notify UI components when commits are created or HEAD changes.
 */
interface FrameworkStorageListener {

  /**
   * Called when a new snapshot is saved to storage.
   * @param ref The ref name that was updated (e.g., "stage_543")
   * @param commitHash The hash of the new commit
   */
  fun snapshotSaved(ref: String, commitHash: String)

  /**
   * Called when HEAD is updated to point to a different ref.
   * @param ref The new HEAD ref, or null if HEAD was cleared
   */
  fun headUpdated(ref: String?)

  companion object {
    @JvmField
    val TOPIC: Topic<FrameworkStorageListener> = Topic.create(
      "Framework Storage Changes",
      FrameworkStorageListener::class.java
    )
  }
}
