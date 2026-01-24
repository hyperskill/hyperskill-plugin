package org.hyperskill.academy.learning.framework.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.util.io.storage.AbstractStorage
import org.hyperskill.academy.learning.framework.storage.Change
import org.hyperskill.academy.learning.framework.storage.FileBasedFrameworkStorage
import org.hyperskill.academy.learning.framework.storage.UserChanges
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class FrameworkStorage(private val storagePath: Path) : Disposable {

  /**
   * Path for new file-based storage.
   * Uses a different directory name to avoid conflict with legacy binary storage.
   * Legacy: .idea/frameworkLessonHistory/storage (file)
   * New:    .idea/frameworkLessonHistory/storage_v3 (directory)
   */
  private val fileBasedStoragePath: Path = storagePath.resolveSibling("storage_v3")

  private var fileBasedStorage: FileBasedFrameworkStorage? = null

  /**
   * Check if legacy binary storage exists.
   * AbstractStorage creates files: "storage", "storage.keystream", "storage.len", "storage.values"
   * We check for the main file being a regular file (not directory).
   */
  fun hasLegacyStorage(): Boolean {
    val mainFile = storagePath.toFile()
    // Legacy storage is a file, new storage is a directory
    return mainFile.exists() && mainFile.isFile
  }

  /**
   * Check if new file-based storage exists.
   * FileBasedFrameworkStorage creates directory with "objects/", "refs/", and "version" file.
   */
  private fun hasFileBasedStorage(): Boolean {
    val dir = fileBasedStoragePath.toFile()
    return dir.exists() && dir.isDirectory && Files.exists(fileBasedStoragePath.resolve("version"))
  }

  private fun getFileBasedStorage(): FileBasedFrameworkStorage {
    return fileBasedStorage ?: FileBasedFrameworkStorage(fileBasedStoragePath).also { fileBasedStorage = it }
  }

  val version: Int
    get() = when {
      hasFileBasedStorage() -> getFileBasedStorage().getVersion()
      hasLegacyStorage() -> 2 // Legacy binary storage, needs migration
      else -> 0 // No storage exists yet
    }

  /**
   * HEAD points to the current stage's ref ID (like git HEAD).
   * Returns -1 if HEAD is not set.
   */
  var head: Int
    get() = if (hasFileBasedStorage()) getFileBasedStorage().getHead() else -1
    set(value) = getFileBasedStorage().setHead(value)

  /**
   * Get the commit hash that HEAD points to.
   * Returns null if HEAD is not set.
   */
  fun getHeadCommit(): String? {
    return if (hasFileBasedStorage()) getFileBasedStorage().getHeadCommit() else null
  }

  /**
   * Get the snapshot that HEAD points to.
   * Returns null if HEAD is not set.
   */
  @Throws(IOException::class)
  fun getHeadSnapshot(): Map<String, String>? {
    return if (hasFileBasedStorage()) getFileBasedStorage().getHeadSnapshot() else null
  }

  /**
   * Get all ref IDs in the storage.
   */
  fun getAllRefIds(): List<Int> {
    return if (hasFileBasedStorage()) getFileBasedStorage().getAllRefIds() else emptyList()
  }

  /**
   * Get information about all refs in the storage.
   */
  fun getAllRefs(): List<FileBasedFrameworkStorage.RefInfo> {
    return if (hasFileBasedStorage()) getFileBasedStorage().getAllRefs() else emptyList()
  }

  /**
   * Get commit by hash.
   */
  fun getCommit(hash: String): FileBasedFrameworkStorage.Commit? {
    return if (hasFileBasedStorage()) {
      try {
        getFileBasedStorage().getCommit(hash)
      } catch (e: Exception) {
        LOG.warn("Failed to get commit $hash", e)
        null
      }
    } else null
  }

  /**
   * Resolve ref ID to commit hash.
   */
  fun resolveRef(refId: Int): String? {
    return if (hasFileBasedStorage()) getFileBasedStorage().resolveRef(refId) else null
  }

  /**
   * Get snapshot by its hash directly (for debugging).
   */
  fun getSnapshotByHash(snapshotHash: String): Map<String, String>? {
    return if (hasFileBasedStorage()) {
      try {
        getFileBasedStorage().getSnapshotByHash(snapshotHash)
      } catch (e: Exception) {
        LOG.warn("Failed to get snapshot by hash $snapshotHash", e)
        null
      }
    } else null
  }

  var isDisposed: Boolean = false
    private set

  constructor(storageFilePath: Path, version: Int) : this(storageFilePath) {
    getFileBasedStorage().setVersion(version)
  }

  /**
   * Get the snapshot (full state) for a ref.
   */
  @Throws(IOException::class)
  fun getSnapshot(refId: Int): Map<String, String> {
    return getFileBasedStorage().getSnapshot(refId)
  }

  /**
   * Get the timestamp when snapshot was saved.
   */
  @Throws(IOException::class)
  fun getSnapshotTimestamp(refId: Int): Long {
    return getFileBasedStorage().getSnapshotTimestamp(refId)
  }

  /**
   * Save a snapshot (full state) and return the new ref ID.
   */
  @Throws(IOException::class)
  fun saveSnapshot(refId: Int, state: Map<String, String>, parentRefId: Int = -1): Int {
    return getFileBasedStorage().saveSnapshot(refId, state, parentRefId)
  }

  @Throws(IOException::class)
  fun migrate(newVersion: Int) {
    val currentVersion = version
    if (currentVersion >= newVersion) return
    getFileBasedStorage().setVersion(newVersion)
  }

  /**
   * Check if legacy storage has changes for a specific ref.
   */
  fun hasLegacyChanges(refId: Int): Boolean {
    if (!hasLegacyStorage()) return false
    return try {
      val legacyStorage = LegacyFrameworkStorage(storagePath)
      try {
        legacyStorage.getRecordInfo(refId) != null
      } finally {
        Disposer.dispose(legacyStorage)
      }
    } catch (e: Exception) {
      LOG.warn("Failed to check legacy changes for ref $refId", e)
      false
    }
  }

  /**
   * Get changes from legacy storage for a specific ref.
   * Returns null if no legacy changes exist.
   */
  fun getLegacyChanges(refId: Int): UserChanges? {
    if (!hasLegacyStorage()) return null
    return try {
      val legacyStorage = LegacyFrameworkStorage(storagePath)
      try {
        when (val refInfo = legacyStorage.getRecordInfo(refId)) {
          is LegacyFrameworkStorage.RecordInfo.Legacy -> refInfo.changes
          is LegacyFrameworkStorage.RecordInfo.Snapshot -> {
            // Convert snapshot to changes (all as AddFile)
            val changes = refInfo.state.map { (path, text) -> Change.AddFile(path, text) }
            UserChanges(changes)
          }
          null -> null
        }
      } finally {
        Disposer.dispose(legacyStorage)
      }
    } catch (e: Exception) {
      LOG.warn("Failed to get legacy changes for ref $refId", e)
      null
    }
  }

  /**
   * Apply legacy changes on top of base state and save as new snapshot.
   * This is used during project open to preserve user's local changes from old storage.
   *
   * @param refId The ref ID (Task.record)
   * @param baseState The base state (e.g., from API or templates)
   * @return The new ref ID after saving, or the original refId if no changes applied
   */
  fun applyLegacyChangesAndSave(refId: Int, baseState: Map<String, String>): Int {
    val legacyChanges = getLegacyChanges(refId) ?: return refId

    LOG.info("Applying legacy changes for ref $refId: ${legacyChanges.changes.size} changes")

    // Apply changes to base state
    val mergedState = baseState.toMutableMap()
    for (change in legacyChanges.changes) {
      when (change) {
        is Change.AddFile -> {
          mergedState[change.path] = change.text
          LOG.info("  AddFile: ${change.path}")
        }
        is Change.ChangeFile -> {
          mergedState[change.path] = change.text
          LOG.info("  ChangeFile: ${change.path}")
        }
        is Change.RemoveFile -> {
          mergedState.remove(change.path)
          LOG.info("  RemoveFile: ${change.path}")
        }
        is Change.PropagateLearnerCreatedTaskFile -> {
          mergedState[change.path] = change.text
          LOG.info("  PropagateLearnerCreatedTaskFile: ${change.path}")
        }
        is Change.RemoveTaskFile -> {
          mergedState.remove(change.path)
          LOG.info("  RemoveTaskFile: ${change.path}")
        }
      }
    }

    // Save merged state as new snapshot
    return saveSnapshot(refId, mergedState)
  }

  /**
   * Mark legacy storage as migrated by deleting it.
   * Call this after all legacy changes have been applied.
   */
  fun deleteLegacyStorage() {
    if (!hasLegacyStorage()) return
    LOG.info("Deleting legacy storage at: $storagePath")
    try {
      AbstractStorage.deleteFiles(storagePath.toString())
      FileUtil.delete(storagePath.toFile())
    } catch (e: Exception) {
      LOG.warn("Failed to delete legacy storage", e)
    }
  }

  override fun dispose() {
    fileBasedStorage?.close()
    isDisposed = true
  }

  fun force() {
    fileBasedStorage?.force()
  }

  fun closeAndClean() {
    fileBasedStorage?.closeAndClean()
    isDisposed = true
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(FrameworkStorage::class.java)

    fun deleteFiles(storagePath: Path) {
      // Delete both legacy and new storage files
      AbstractStorage.deleteFiles(storagePath.toString())
      FileUtil.delete(storagePath.toFile())
      // Also delete new storage directory
      val newStoragePath = storagePath.resolveSibling("storage_v3")
      FileUtil.delete(newStoragePath.toFile())
    }
  }
}

