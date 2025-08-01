package org.hyperskill.academy.learning.json.mixins

object JsonMixinNames {
  // common constants
  const val TYPE = "type"
  const val NAME = "name"
  const val TITLE = "title"
  const val ITEMS = "items"
  const val TAGS = "tags"
  const val ITEM_TYPE = "type"

  // course
  const val LANGUAGE = "language"
  const val SUMMARY = "summary"

  @Deprecated("Use PROGRAMMING_LANGUAGE_ID and PROGRAMMING_LANGUAGE_VERSION instead")
  const val PROGRAMMING_LANGUAGE = "programming_language"
  const val PROGRAMMING_LANGUAGE_ID = "programming_language_id"
  const val PROGRAMMING_LANGUAGE_VERSION = "programming_language_version"
  const val COURSE_TYPE = "course_type"
  const val ENVIRONMENT = "environment"
  const val ENVIRONMENT_SETTINGS = "environment_settings"
  const val DESCRIPTION_TEXT = "description_text"
  const val DESCRIPTION_FORMAT = "description_format"
  const val ADDITIONAL_FILES = "additional_files"
  const val CUSTOM_CONTENT_PATH = "custom_content_path"
  const val DISABLED_FEATURES = "disabled_features"
  const val SOLUTIONS_HIDDEN = "solutions_hidden"
  const val VERSION = "version"

  //plugin dependency
  const val PLUGIN_NAME = "plugin_name"
  const val PLUGIN_ID = "id"
  const val MAX_VERSION = "max_version"
  const val MIN_VERSION = "min_version"

  // lesson
  const val TASK_LIST = "task_list"
  const val FRAMEWORK_TYPE = "framework"

  // guided project
  const val IS_TEMPLATE_BASED = "is_template_based"

  // task
  const val FILES = "files"
  const val TASK_TYPE = "task_type"
  const val CUSTOM_NAME = "custom_name"
  const val SOLUTION_HIDDEN = "solution_hidden"

  // task file
  const val TEXT = "text"
  const val IS_BINARY = "is_binary"
  const val IS_VISIBLE = "is_visible"
  const val PLACEHOLDERS = "placeholders"
  const val IS_EDITABLE = "is_editable"
  const val HIGHLIGHT_LEVEL = "highlight_level"

  // feedback
  const val FEEDBACK_LINK = "feedback_link"

  // placeholder
  const val DEPENDENCY = "dependency"

  // placeholder dependency
  const val SECTION = "section"
  const val LESSON = "lesson"
  const val TASK = "task"
  const val FILE = "file"

  // remote study item
  const val UPDATE_DATE = "update_date"
  const val ID = "id"

  // submissions
  const val STATUS = "status"
}