package com.jetbrains.edu.learning.yaml.format

import com.jetbrains.edu.learning.json.mixins.JsonMixinNames


object YamlMixinNames {

  // common constants
  const val EDU_YAML_TYPE = "edu"
  const val TYPE = JsonMixinNames.TYPE
  const val CONTENT = "content"
  const val CUSTOM_NAME = JsonMixinNames.CUSTOM_NAME
  const val TAGS = JsonMixinNames.TAGS

  // course
  const val TITLE = JsonMixinNames.TITLE
  const val LANGUAGE = JsonMixinNames.LANGUAGE
  const val SUMMARY = JsonMixinNames.SUMMARY
  const val PROGRAMMING_LANGUAGE = "programming_language"
  const val PROGRAMMING_LANGUAGE_VERSION = JsonMixinNames.PROGRAMMING_LANGUAGE_VERSION
  const val SOLUTIONS_HIDDEN = JsonMixinNames.SOLUTIONS_HIDDEN
  const val MODE = "mode"
  const val ENVIRONMENT = JsonMixinNames.ENVIRONMENT
  const val ENVIRONMENT_SETTINGS = JsonMixinNames.ENVIRONMENT_SETTINGS
  const val ADDITIONAL_FILES = JsonMixinNames.ADDITIONAL_FILES
  const val CUSTOM_CONTENT_PATH = JsonMixinNames.CUSTOM_CONTENT_PATH

  const val YAML_VERSION = "yaml_version"

  //hyperskill course
  const val HYPERSKILL_PROJECT = "hyperskill_project"
  const val STAGES = "stages"
  const val THEORY_ID = "theory_id"
  const val HYPERSKILL_TYPE_YAML = "hyperskill"
  const val END_DATE_TIME = "end_date_time"

  // framework lesson
  const val CURRENT_TASK = "current_task"
  const val IS_TEMPLATE_BASED = JsonMixinNames.IS_TEMPLATE_BASED

  // task
  const val FILES = JsonMixinNames.FILES
  const val FEEDBACK_LINK = JsonMixinNames.FEEDBACK_LINK
  const val FEEDBACK = "feedback"
  const val STATUS = "status"
  const val RECORD = "record"
  const val SOLUTION_HIDDEN = JsonMixinNames.SOLUTION_HIDDEN
  const val SUBMISSION_LANGUAGE = "submission_language"

  // theory task
  const val POST_SUBMISSION_ON_OPEN = "post_submission_on_open"

  // feedback
  const val MESSAGE = "message"
  const val TIME = "time"
  const val EXPECTED = "expected"
  const val ACTUAL = "actual"

  // task file
  const val NAME = JsonMixinNames.NAME
  const val PLACEHOLDERS = JsonMixinNames.PLACEHOLDERS
  const val VISIBLE = "visible"
  const val LEARNER_CREATED = "learner_created"
  const val TEXT = JsonMixinNames.TEXT
  const val ENCRYPTED_TEXT = "encrypted_text"
  const val IS_BINARY = "is_binary"
  const val EDITABLE = "editable"
  const val PROPAGATABLE = "propagatable"
  const val HIGHLIGHT_LEVEL = JsonMixinNames.HIGHLIGHT_LEVEL

  // placeholder dependency
  const val SECTION = JsonMixinNames.SECTION
  const val LESSON = JsonMixinNames.LESSON
  const val TASK = JsonMixinNames.TASK

  // remote study item
  const val ID = JsonMixinNames.ID
  const val UPDATE_DATE = JsonMixinNames.UPDATE_DATE
}
