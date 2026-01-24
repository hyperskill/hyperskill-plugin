package org.hyperskill.academy.learning.framework.storage

import java.io.DataInput
import java.io.DataOutput
import java.io.EOFException
import java.io.IOException

class UserChanges(val changes: List<Change>, val timestamp: Long = System.currentTimeMillis()) : FrameworkStorageData {

  operator fun plus(otherChanges: List<Change>): UserChanges = UserChanges(changes + otherChanges)

  fun apply(state: MutableMap<String, String>) {
    for (change in changes) {
      change.apply(state)
    }
  }

  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    FrameworkStorageUtils.writeINT(out, changes.size)
    changes.forEach { Change.writeChange(it, out) }
    FrameworkStorageUtils.writeLONG(out, timestamp)
  }

  companion object {
    private val EMPTY = UserChanges(emptyList(), -1)

    fun empty(): UserChanges = EMPTY

    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges {
      return try {
        val size = FrameworkStorageUtils.readINT(input)
        // Guard against corrupted data: reasonable limit for number of files in a task
        if (size < 0 || size > 10000) {
          throw IOException("Corrupted data: invalid number of changes $size")
        }
        val changes = ArrayList<Change>(size)
        for (i in 0 until size) {
          changes += Change.readChange(input)
        }
        val timestamp = FrameworkStorageUtils.readLONG(input)
        UserChanges(changes, timestamp)
      } catch (e: EOFException) {
        EMPTY
      } catch (e: Exception) {
        if (e is IOException) throw e
        throw IOException("Corrupted data during UserChanges read", e)
      }
    }
  }
}

sealed class Change {

  val path: String
  val text: String

  constructor(path: String, text: String) {
    this.path = path
    this.text = text
  }

  @Throws(IOException::class)
  constructor(input: DataInput) {
    this.path = input.readUTF()
    this.text = Companion.readString(input)
  }

  @Throws(IOException::class)
  protected fun write(out: DataOutput) {
    out.writeUTF(path)
    Companion.writeString(out, text)
  }

  abstract fun apply(state: MutableMap<String, String>)

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other !is Change) return false
    if (javaClass != other.javaClass) return false

    if (path != other.path) return false
    if (text != other.text) return false

    return true
  }

  override fun hashCode(): Int {
    var result = path.hashCode()
    result = 31 * result + text.hashCode()
    return result
  }

  override fun toString(): String {
    return "${javaClass.simpleName}(path='$path', text='$text')"
  }

  class AddFile : Change {
    constructor(path: String, text: String) : super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)
    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class RemoveFile : Change {
    constructor(path: String) : super(path, "")
    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)
    override fun apply(state: MutableMap<String, String>) {
      state -= path
    }
  }

  class ChangeFile : Change {
    constructor(path: String, text: String) : super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)
    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class PropagateLearnerCreatedTaskFile : Change {
    constructor(path: String, text: String) : super(path, text)
    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)
    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class RemoveTaskFile : Change {
    constructor(path: String) : super(path, "")
    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)
    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  companion object {
    private const val UTF_ENCODING_THRESHOLD = 65535
    private const val EXTENDED_FORMAT_MARKER = "\u0000EXTENDED\u0000"
    private const val MAX_STRING_LENGTH = 100 * 1024 * 1024

    @Throws(IOException::class)
    fun writeChange(change: Change, out: DataOutput) {
      val ordinal = when (change) {
        is AddFile -> 0
        is RemoveFile -> 1
        is ChangeFile -> 2
        is PropagateLearnerCreatedTaskFile -> 3
        is RemoveTaskFile -> 4
      }
      out.writeInt(ordinal)
      change.write(out)
    }

    @Throws(IOException::class)
    fun readChange(input: DataInput): Change {
      val ordinal = input.readInt()
      return when (ordinal) {
        0 -> AddFile(input)
        1 -> RemoveFile(input)
        2 -> ChangeFile(input)
        3 -> PropagateLearnerCreatedTaskFile(input)
        4 -> RemoveTaskFile(input)
        else -> error("Unexpected change type: $ordinal")
      }
    }

    @Throws(IOException::class)
    fun writeString(out: DataOutput, value: String) {
      val bytes = value.toByteArray(Charsets.UTF_8)
      if (bytes.size < UTF_ENCODING_THRESHOLD) {
        out.writeUTF(value)
      } else {
        out.writeUTF(EXTENDED_FORMAT_MARKER)
        FrameworkStorageUtils.writeINT(out, bytes.size)
        out.write(bytes)
      }
    }

    @Throws(IOException::class)
    fun readString(input: DataInput): String {
      val utfString = input.readUTF()
      if (utfString != EXTENDED_FORMAT_MARKER) {
        return utfString
      }
      val length = FrameworkStorageUtils.readINT(input)
      if (length < 0 || length > MAX_STRING_LENGTH) {
        throw IOException("Corrupted data: invalid string length $length")
      }
      val bytes = ByteArray(length)
      input.readFully(bytes)
      return String(bytes, Charsets.UTF_8)
    }
  }
}
