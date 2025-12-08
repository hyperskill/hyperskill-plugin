package org.hyperskill.academy.learning.exceptions

import com.intellij.openapi.util.text.StringUtil
import org.hyperskill.academy.learning.messages.EduCoreBundle

/**
 * Default content load limit in bytes (2.5 MB).
 * This matches the default value from IntelliJ Platform's FileSizeLimit.
 */
private const val DEFAULT_CONTENT_LOAD_LIMIT = 2_500_000

/**
 * [HugeBinaryFileException] is similar to [com.intellij.openapi.util.io.FileTooBigException],
 * but stores additional information and generates a user visible message.
 */
class HugeBinaryFileException(val path: String, val size: Long, val limit: Long, insideFrameworkLesson: Boolean = false) :
  IllegalStateException(
    buildString {
      appendLine(EduCoreBundle.message("error.educator.file.size", path))
      appendLine(EduCoreBundle.message("error.educator.file.size.limit", StringUtil.formatFileSize(size), StringUtil.formatFileSize(limit)))
      if (insideFrameworkLesson && size <= DEFAULT_CONTENT_LOAD_LIMIT) {
        appendLine(EduCoreBundle.message("error.educator.file.size.workaround.framework"))
      }
      else {
        appendLine(EduCoreBundle.message("error.educator.file.size.workaround.exclude"))
      }
    }
  )