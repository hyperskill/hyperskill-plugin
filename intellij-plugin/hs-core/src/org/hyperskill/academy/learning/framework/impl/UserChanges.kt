package org.hyperskill.academy.learning.framework.impl

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.io.DataInputOutputUtil
import org.apache.commons.codec.binary.Base64
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.courseGeneration.macro.EduMacroUtils
import java.io.DataInput
import java.io.DataOutput
import java.io.EOFException
import java.io.IOException

class UserChanges(val changes: List<Change>, val timestamp: Long = System.currentTimeMillis()) : FrameworkStorageData {

  operator fun plus(otherChanges: List<Change>): UserChanges = UserChanges(changes + otherChanges)

  fun apply(project: Project, taskDir: VirtualFile, task: Task) {
    LOG.warn("UserChanges.apply: applying ${changes.size} changes to taskDir=${taskDir.path}")
    for (change in changes) {
      LOG.warn("UserChanges.apply: change=${change.javaClass.simpleName}(${change.path}, ${change.text.length} chars)")
      change.apply(project, taskDir, task)
    }
  }

  fun apply(state: MutableMap<String, String>) {
    for (change in changes) {
      change.apply(state)
    }
  }

  @Throws(IOException::class)
  override fun write(out: DataOutput) {
    DataInputOutputUtil.writeINT(out, changes.size)
    changes.forEach { Change.writeChange(it, out) }
    DataInputOutputUtil.writeLONG(out, timestamp)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(UserChanges::class.java)

    private val EMPTY = UserChanges(emptyList(), -1)

    fun empty(): UserChanges = EMPTY

    @Throws(IOException::class)
    fun read(input: DataInput): UserChanges {
      return try {
        val size = DataInputOutputUtil.readINT(input)
        val changes = ArrayList<Change>(size)
        for (i in 0 until size) {
          changes += Change.readChange(input)
        }
        val timestamp = DataInputOutputUtil.readLONG(input)
        UserChanges(changes, timestamp)
      } catch (e: EOFException) {
        // Storage file is corrupted or empty, return empty changes
        EMPTY
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


  abstract fun apply(project: Project, taskDir: VirtualFile, task: Task)
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

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      if (task.getTaskFile(path) == null) {
        GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, path, InMemoryTextualContents(text))
      }
      else {
        try {
          EduDocumentListener.modifyWithoutListener(task, path) {
            GeneratorUtils.createChildFile(project.toCourseInfoHolder(), taskDir, path, InMemoryTextualContents(text))
          }
        }
        catch (e: IOException) {
          LOG.error("Failed to create file `${taskDir.path}/$path`", e)
        }
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class RemoveFile : Change {

    constructor(path: String) : super(path, "")

    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      runUndoTransparentWriteAction {
        try {
          taskDir.findFileByRelativePath(path)?.removeWithEmptyParents(taskDir)
        }
        catch (e: IOException) {
          LOG.error("Failed to delete file `${taskDir.path}/$path`", e)
        }
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state -= path
    }
  }

  class ChangeFile : Change {

    constructor(path: String, text: String) : super(path, text)

    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      LOG.warn("ChangeFile.apply: path=$path, taskDir=${taskDir.path}, textLength=${text.length}")
      val file = taskDir.findFileByRelativePath(path)
      if (file == null) {
        LOG.warn("ChangeFile.apply: Can't find file `$path` in `$taskDir`")
        return
      }
      LOG.warn("ChangeFile.apply: Found file at ${file.path}")

      if (file.isToEncodeContent) {
        LOG.warn("ChangeFile.apply: Using binary content mode")
        file.doWithoutReadOnlyAttribute {
          runWriteAction {
            file.setBinaryContent(Base64.decodeBase64(text))
          }
        }
      }
      else {
        LOG.warn("ChangeFile.apply: Using document mode")
        EduDocumentListener.modifyWithoutListener(task, path) {
          val document = runReadAction { FileDocumentManager.getInstance().getDocument(file) }
          if (document != null) {
            val expandedText = StringUtil.convertLineSeparators(EduMacroUtils.expandMacrosForFile(project.toCourseInfoHolder(), file, text))
            LOG.warn("ChangeFile.apply: Setting document text, expandedTextLength=${expandedText.length}")
            file.doWithoutReadOnlyAttribute {
              runUndoTransparentWriteAction { document.setText(expandedText) }
            }
            // ALT-10961: Force save document to disk
            FileDocumentManager.getInstance().saveDocument(document)
            LOG.warn("ChangeFile.apply: Document text set and saved successfully")
          }
          else {
            LOG.warn("ChangeFile.apply: Can't get document for `$file`")
          }
        }
      }
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class PropagateLearnerCreatedTaskFile : Change {

    constructor(path: String, text: String) : super(path, text)

    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      val taskFile = TaskFile(path, text).apply { isLearnerCreated = true }
      task.addTaskFile(taskFile)
    }


    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  class RemoveTaskFile : Change {

    constructor(path: String) : super(path, "")

    @Throws(IOException::class)
    constructor(input: DataInput) : super(input)

    override fun apply(project: Project, taskDir: VirtualFile, task: Task) {
      task.removeTaskFile(path)
    }

    override fun apply(state: MutableMap<String, String>) {
      state[path] = text
    }
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(Change::class.java)
    private const val UTF_ENCODING_THRESHOLD = 65535

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

    // Marker for extended format - a string that won't appear in normal content
    private const val EXTENDED_FORMAT_MARKER = "\u0000EXTENDED\u0000"

    @Throws(IOException::class)
    fun writeString(out: DataOutput, value: String) {
      val bytes = value.toByteArray(Charsets.UTF_8)
      if (bytes.size < UTF_ENCODING_THRESHOLD) {
        // Use standard UTF format for small strings (backward compatible)
        // For empty strings, this correctly writes an empty UTF string
        out.writeUTF(value)
      }
      else {
        // For large strings, write a special marker, then length + bytes
        out.writeUTF(EXTENDED_FORMAT_MARKER)
        DataInputOutputUtil.writeINT(out, bytes.size)
        out.write(bytes)
      }
    }

    @Throws(IOException::class)
    fun readString(input: DataInput): String {
      val utfString = input.readUTF()
      if (utfString != EXTENDED_FORMAT_MARKER) {
        // Standard UTF format (including empty strings)
        return utfString
      }
      // Extended format: read length + bytes
      val length = DataInputOutputUtil.readINT(input)
      if (length < 0 || length > MAX_STRING_LENGTH) {
        throw IOException("Corrupted data: invalid string length $length")
      }
      val bytes = ByteArray(length)
      input.readFully(bytes)
      return String(bytes, Charsets.UTF_8)
    }

    // Maximum reasonable string length (100 MB) to detect corruption
    private const val MAX_STRING_LENGTH = 100 * 1024 * 1024
  }
}
