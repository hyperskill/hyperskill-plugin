package org.hyperskill.academy.coursecreator.actions

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.courseFormat.BinaryContents
import org.hyperskill.academy.learning.courseFormat.TextualContents
import org.hyperskill.academy.learning.exceptions.HugeBinaryFileException

/**
 * Default content load limit in bytes (2.5 MB).
 * This matches the default value from IntelliJ Platform's FileSizeLimit.
 */
private const val DEFAULT_CONTENT_LOAD_LIMIT = 2_500_000L

class BinaryContentsFromDisk(val file: VirtualFile) : BinaryContents {
  override val bytes: ByteArray
    get() = runReadAction {
      try {
        file.contentsToByteArray()
      }
      catch (_: FileTooBigException) {
        throw HugeBinaryFileException(file.path, file.length, DEFAULT_CONTENT_LOAD_LIMIT)
      }
    }
}

class TextualContentsFromDisk(val file: VirtualFile) : TextualContents {
  override val text: String
    get() = runReadAction {
      VfsUtilCore.loadText(file)
    }
}