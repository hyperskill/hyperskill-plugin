@file:Suppress("unused", "PropertyName") // used for json serialization

package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.text.StringUtil
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.ID
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.IS_VISIBLE
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.NAME
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.STATUS
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.TEXT
import com.jetbrains.edu.learning.json.mixins.NotImplementedInMixin
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import java.util.*

const val IS_IDEA_COMPATIBLE = "is_idea_compatible"
const val IS_ADAPTIVE = "is_adaptive"
const val COURSE_FORMAT = "course_format"
const val SECTIONS = "sections"
const val INSTRUCTORS = "instructors"
const val UPDATE_DATE = "update_date"
const val IS_PUBLIC = "is_public"
const val SUMMARY = "summary"
const val TITLE = "title"
const val PROGRAMMING_LANGUAGE = "programming_language"
const val LANGUAGE = "language"
const val ADMINS_GROUP = "admins_group"
const val LEARNERS_COUNT = "learners_count"
const val REVIEW_SUMMARY = "review_summary"
const val UNITS = "units"
const val COURSE = "course"
const val POSITION = "position"
const val STEPS = "steps"
const val NUMBER = "number"
const val OFFSET = "offset"
const val LENGTH = "length"
const val DEPENDENCY = "dependency"
const val POSSIBLE_ANSWER = "possible_answer"
const val PLACEHOLDER_TEXT = "placeholder_text"
const val SELECTED = "selected"
const val SECTION = "section"
const val LESSON = "lesson"
const val TASK = "task"
const val PLACEHOLDER = "placeholder"
const val STEPIK_ID = "stepic_id"
const val FILES = "files"
const val CHOICE_VARIANTS = "choice_variants"
const val IS_MULTICHOICE = "is_multichoice"
const val SELECTED_VARIANTS = "selected_variants"
const val CUSTOM_NAME = "custom_name"
const val SOLUTION_HIDDEN = "solution_hidden"
private const val TASK_TYPE = "task_type"
private const val ENVIRONMENT_SEPARATOR = "#"

open class StepikItemMixin {
  @JsonProperty(ID)
  var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  lateinit var updateDate: Date
}

class StepikLessonMixin : StepikItemMixin() {
  @JsonProperty(STEPS)
  lateinit var stepIds: MutableList<Int>

  @JsonProperty(TITLE)
  lateinit var name: String

}

@JsonPropertyOrder(NAME, IS_VISIBLE, TEXT)
open class StepikEduFileMixin {
  @JsonProperty(NAME)
  lateinit var name: String

  @JsonProperty(IS_VISIBLE)
  var isVisible = true

  lateinit var text: String
    @JsonProperty(TEXT)
    @JsonDeserialize(using = TaskFileTextDeserializer::class)
    get
    @JsonProperty(TEXT)
    set
}

@JsonPropertyOrder(NAME, STEPIK_ID, STATUS, FILES, TASK_TYPE)
open class StepikTaskMixin {
  @JsonProperty(NAME)
  var name: String? = null

  @JsonProperty(STATUS)
  var checkStatus = CheckStatus.Unchecked

  @JsonProperty(STEPIK_ID)
  private var id: Int = 0

  @JsonProperty(FILES)
  private var _taskFiles: MutableMap<String, TaskFile> = LinkedHashMap()

  val itemType: String
    @JsonProperty(TASK_TYPE)
    get() = throw NotImplementedInMixin()
}

class TaskFileTextDeserializer(vc: Class<*>? = null) : StdDeserializer<String>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): String {
    return StringUtil.convertLineSeparators(jp.valueAsString)
  }
}

private val LOG: Logger = logger<HyperskillConnector>()
