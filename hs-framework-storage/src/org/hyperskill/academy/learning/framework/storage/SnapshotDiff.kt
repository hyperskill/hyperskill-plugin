package org.hyperskill.academy.learning.framework.storage

/**
 * Utility for calculating differences between snapshots.
 * Works like `git diff` - compares two states and produces a change list.
 */
object SnapshotDiff {

  /**
   * Calculate changes needed to transform [initialState] into [currentState].
   *
   * @param initialState The base state (e.g., template files)
   * @param currentState The target state (e.g., user's snapshot)
   * @return List of changes (AddFile, ChangeFile, RemoveFile)
   */
  fun calculateChanges(
    initialState: Map<String, String>,
    currentState: Map<String, String>
  ): UserChanges {
    val changes = mutableListOf<Change>()

    // Files in currentState
    for ((path, currentText) in currentState) {
      val initialText = initialState[path]
      when {
        initialText == null -> {
          // File not in initial state → user added it
          changes += Change.AddFile(path, currentText)
        }
        initialText != currentText -> {
          // File exists in both but content differs → user modified it
          changes += Change.ChangeFile(path, currentText)
        }
        // else: file unchanged, no change needed
      }
    }

    // Files in initialState but not in currentState → user deleted them
    for (path in initialState.keys) {
      if (path !in currentState) {
        changes += Change.RemoveFile(path)
      }
    }

    return UserChanges(changes)
  }

  /**
   * Apply changes to a state map, returning the resulting state.
   *
   * @param initialState The base state to apply changes to
   * @param changes The changes to apply
   * @return New state after applying changes
   */
  fun applyChanges(
    initialState: Map<String, String>,
    changes: UserChanges
  ): Map<String, String> {
    val result = initialState.toMutableMap()
    for (change in changes.changes) {
      when (change) {
        is Change.AddFile -> result[change.path] = change.text
        is Change.ChangeFile -> result[change.path] = change.text
        is Change.RemoveFile -> result.remove(change.path)
        is Change.PropagateLearnerCreatedTaskFile -> result[change.path] = change.text
        is Change.RemoveTaskFile -> result.remove(change.path)
      }
    }
    return result
  }
}
