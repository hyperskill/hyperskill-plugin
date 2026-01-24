package org.hyperskill.academy.learning.framework.impl

import com.intellij.util.io.DataInputOutputUtil
import com.intellij.util.io.UnsyncByteArrayInputStream
import com.intellij.util.io.UnsyncByteArrayOutputStream
import org.hyperskill.academy.learning.framework.impl.migration.To1VersionRecordConverter
import org.hyperskill.academy.learning.framework.storage.Change
import org.hyperskill.academy.learning.framework.storage.UserChanges
import java.io.DataInput
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/**
 * Legacy binary storage reader for migration purposes only.
 * Uses IntelliJ's AbstractStorage format with DataInputOutputUtil for VLQ encoding.
 * After migration to file-based storage (version 3+), this class is no longer used.
 */
class LegacyFrameworkStorage(storagePath: Path) : FrameworkStorageBase(storagePath) {

  private val contentHashIndex = ConcurrentHashMap<String, Int>()

  init {
    try {
      withReadLock<Unit, IOException> {
        val recordIterator = myRecordsTable.createRecordIdIterator()
        while (recordIterator.hasNextId()) {
          val recordId = recordIterator.nextId()
          val bytes = readBytes(recordId)
          if (bytes.isNotEmpty() && bytes[0].toInt() == BLOB_TYPE) {
            val input = DataInputStream(java.io.ByteArrayInputStream(bytes))
            input.readByte() // skip type
            val hash = input.readUTF()
            contentHashIndex[hash] = recordId
          }
        }
      }
    } catch (e: Exception) {
      // It's okay if index population fails
    }
  }

  fun getRecordIterator() = myRecordsTable.createRecordIdIterator()

  sealed class RecordInfo {
    data class Legacy(val changes: UserChanges) : RecordInfo()
    data class Snapshot(val state: Map<String, String>) : RecordInfo()
  }

  @Throws(IOException::class)
  fun getRecordInfo(recordId: Int): RecordInfo? {
    return withReadLock<RecordInfo?, IOException> {
      val bytes = readBytes(recordId)
      if (bytes.isEmpty()) return@withReadLock null
      val input = DataInputStream(java.io.ByteArrayInputStream(bytes))
      val type = input.readByte().toInt()
      when (type) {
        LEGACY_CHANGES_TYPE -> RecordInfo.Legacy(readLegacyUserChanges(input))
        SNAPSHOT_TYPE -> {
          val snapshot = readSnapshot(input)
          val state = snapshot.mapValues { (_, contentId) -> readBlob(contentId) }
          RecordInfo.Snapshot(state)
        }
        else -> null
      }
    }
  }

  /**
   * Read UserChanges using IntelliJ's DataInputOutputUtil VLQ format.
   * This is the format used in legacy binary storage (versions 0-2).
   * Must use DataInputOutputUtil for compatibility with data written by old versions.
   */
  @Throws(IOException::class)
  private fun readLegacyUserChanges(input: DataInput): UserChanges {
    return try {
      val size = DataInputOutputUtil.readINT(input)
      if (size < 0 || size > 10000) {
        throw IOException("Corrupted data: invalid number of changes $size")
      }
      val changes = ArrayList<Change>(size)
      for (i in 0 until size) {
        changes += Change.readChange(input)
      }
      val timestamp = DataInputOutputUtil.readLONG(input)
      UserChanges(changes, timestamp)
    } catch (e: EOFException) {
      UserChanges.empty()
    } catch (e: Exception) {
      if (e is IOException) throw e
      throw IOException("Corrupted data during legacy UserChanges read", e)
    }
  }

  @Throws(IOException::class)
  fun getUserChanges(record: Int): UserChanges {
    return if (record == -1) {
      UserChanges.empty()
    }
    else {
      withReadLock<UserChanges, IOException> {
        val bytes = readBytes(record)
        val input = DataInputStream(java.io.ByteArrayInputStream(bytes))
        val type = input.readByte().toInt()
        if (type == LEGACY_CHANGES_TYPE) {
          readLegacyUserChanges(input)
        } else if (type == SNAPSHOT_TYPE) {
          // Reconstruct UserChanges from snapshot
          val snapshot = readSnapshot(input)
          val changes = snapshot.map { (path, contentId) ->
            val text = readBlob(contentId)
            Change.AddFile(path, text)
          }
          UserChanges(changes)
        } else {
          throw IOException("Corrupted data: unknown record type: $type")
        }
      }
    }
  }

  @Throws(IOException::class)
  private fun readBlob(recordId: Int): String {
    val bytes = readBytes(recordId)
    val input = DataInputStream(java.io.ByteArrayInputStream(bytes))
    val type = input.readByte().toInt()
    if (type != BLOB_TYPE) throw IOException("Corrupted data: record $recordId is not a blob (type=$type)")
    val hash = input.readUTF()
    val length = DataInputOutputUtil.readINT(input)
    if (length < 0 || length > MAX_BLOB_SIZE) {
      throw IOException("Corrupted data: invalid blob size $length")
    }
    val contentBytes = ByteArray(length)
    input.readFully(contentBytes)
    return String(contentBytes, StandardCharsets.UTF_8)
  }

  @Throws(IOException::class)
  private fun readSnapshot(input: DataInputStream): Map<String, Int> {
    val size = DataInputOutputUtil.readINT(input)
    if (size < 0 || size > MAX_SNAPSHOT_SIZE) {
      throw IOException("Corrupted data: invalid snapshot size $size")
    }
    val snapshot = mutableMapOf<String, Int>()
    for (i in 0 until size) {
      val path = input.readUTF()
      val contentId = DataInputOutputUtil.readINT(input)
      snapshot[path] = contentId
    }
    return snapshot
  }

  @Throws(IOException::class)
  override fun migrateRecord(recordId: Int, currentVersion: Int, newVersion: Int) {
    var v = currentVersion
    if (v >= newVersion) return

    val bytes = readBytes(recordId)
    if (bytes.isEmpty()) return

    var currentBytes = bytes
    var lastOutput: UnsyncByteArrayOutputStream? = null

    while (v < newVersion) {
      val nextOutput = UnsyncByteArrayOutputStream()
      when (v) {
        0 -> {
          To1VersionRecordConverter().convert(DataInputStream(UnsyncByteArrayInputStream(currentBytes)), DataOutputStream(nextOutput))
        }
        1 -> {
          // Version 1 to 2: Add LEGACY_CHANGES_TYPE prefix to UserChanges
          val out = DataOutputStream(nextOutput)
          out.writeByte(LEGACY_CHANGES_TYPE)
          out.write(currentBytes)
        }
        else -> {
          nextOutput.write(currentBytes)
        }
      }
      lastOutput = nextOutput
      currentBytes = nextOutput.toByteArray()
      v++
    }

    if (lastOutput != null) {
      writeBytes(recordId, lastOutput.toByteArraySequence(), false)
    }
  }

  companion object {
    private const val LEGACY_CHANGES_TYPE = 0
    private const val BLOB_TYPE = 1
    private const val SNAPSHOT_TYPE = 2

    private const val MAX_BLOB_SIZE = 100 * 1024 * 1024 // 100 MB
    private const val MAX_SNAPSHOT_SIZE = 10000
  }
}
