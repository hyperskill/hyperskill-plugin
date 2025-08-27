package org.hyperskill.academy.coursecreator.actions

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.io.FileTooBigException
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.limits.FileSizeLimit
import org.hyperskill.academy.learning.courseFormat.BinaryContents
import org.hyperskill.academy.learning.courseFormat.TextualContents
import org.hyperskill.academy.learning.exceptions.HugeBinaryFileException

class BinaryContentsFromDisk(val file: VirtualFile) : BinaryContents {
  override val bytes: ByteArray
    get() = runReadAction {
      try {
        file.contentsToByteArray()
      }
      catch (_: FileTooBigException) {
        throw HugeBinaryFileException(file.path, file.length, FileSizeLimit.getDefaultContentLoadLimit().toLong())
      }
    }
}

class TextualContentsFromDisk(val file: VirtualFile) : TextualContents {
  override val text: String
    get() = runReadAction {
      VfsUtilCore.loadText(file)
    }
}