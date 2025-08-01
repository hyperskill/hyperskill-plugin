package org.hyperskill.academy.learning.serialization

object SerializationUtils {
  const val LINE = "line"
  const val START = "start"
  const val HINT = "hint"
  const val ADDITIONAL_HINTS = "additional_hints"
  const val OFFSET = "offset"
  const val ID = "id"
  const val STATUS = "status"
  const val SUBTASK_MARKER = "_subtask"

  object Json {
    const val FILES = "files"
    const val TESTS = "test"
    const val TEXTS = "text"
    const val HINTS = "hints"
    const val SUBTASK_INFOS = "subtask_infos"
    const val FORMAT_VERSION = "format_version"
    const val INDEX = "index"
    const val TASK_TYPE = "task_type"
    const val NAME = "name"
    const val TITLE = "title"
    const val LAST_SUBTASK = "last_subtask_index"
    const val ITEM_TYPE = "type"
    const val PLACEHOLDERS = "placeholders"
    const val POSSIBLE_ANSWER = "possible_answer"
    const val PLACEHOLDER_TEXT = "placeholder_text"
    const val FILE_WRAPPER_TEXT = "text"
    const val DESCRIPTION_TEXT = "description_text"
    const val DESCRIPTION_FORMAT = "description_format"
    const val ADDITIONAL_FILES = "additional_files"
    const val TEXT = "text"
    const val IS_VISIBLE = "is_visible"
    const val DEPENDENCY = "dependency"
    const val DEPENDENCY_FILE = "file"
  }
}
