package org.hyperskill.academy.learning.json.mixins

import com.fasterxml.jackson.annotation.JacksonInject
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.ObjectCodec
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.ObjectNode
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.*
import org.hyperskill.academy.learning.json.encrypt.Encrypt
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.CHOICE_OPTIONS
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.DESCRIPTION_FORMAT
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.DESCRIPTION_TEXT
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.FEEDBACK_LINK
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.FILES
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.FRAMEWORK_TYPE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.HIGHLIGHT_LEVEL
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.IS_BINARY
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.IS_EDITABLE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.IS_MULTIPLE_CHOICE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.IS_VISIBLE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.ITEMS
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.ITEM_TYPE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.LESSON
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.MAX_VERSION
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.MESSAGE_CORRECT
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.MESSAGE_INCORRECT
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.MIN_VERSION
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.NAME
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.PLUGIN_ID
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.PLUGIN_NAME
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.QUIZ_HEADER
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.SECTION
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.SOLUTION_HIDDEN
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.STATUS
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TAGS
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TASK_LIST
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TASK_TYPE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TEXT
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TITLE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.TYPE


abstract class PluginInfoMixin : PluginInfo() {
  @JsonProperty(PLUGIN_ID)
  override var stringId: String = ""

  @JsonProperty(PLUGIN_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var displayName: String? = null

  @JsonProperty(MIN_VERSION)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var minVersion: String? = null

  @JsonProperty(MAX_VERSION)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  override var maxVersion: String? = null
}

@JsonPropertyOrder(TITLE, CUSTOM_NAME, TAGS, ITEMS, TYPE)
abstract class LocalSectionMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(ITEMS)
  private lateinit var _items: List<StudyItem>

  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(TITLE, CUSTOM_NAME, TAGS, TASK_LIST, TYPE)
abstract class LocalLessonMixin {
  @JsonProperty(TITLE)
  private lateinit var name: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(TASK_LIST)
  private lateinit var _items: List<StudyItem>

  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(
  NAME, CUSTOM_NAME, TAGS, FILES, DESCRIPTION_TEXT, DESCRIPTION_FORMAT, FEEDBACK_LINK, SOLUTION_HIDDEN,
  TASK_TYPE
)
abstract class LocalTaskMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(FILES)
  private lateinit var _taskFiles: MutableMap<String, TaskFile>

  @JsonProperty(DESCRIPTION_TEXT)
  private lateinit var descriptionText: String

  @JsonProperty(DESCRIPTION_FORMAT)
  private lateinit var descriptionFormat: DescriptionFormat

  @JsonProperty(FEEDBACK_LINK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private lateinit var feedbackLink: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var solutionHidden: Boolean? = null

  val itemType: String
    @JsonProperty(TASK_TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPropertyOrder(
  CHOICE_OPTIONS, IS_MULTIPLE_CHOICE, MESSAGE_CORRECT, MESSAGE_INCORRECT, QUIZ_HEADER,
  NAME, CUSTOM_NAME, TAGS, FILES, DESCRIPTION_TEXT, DESCRIPTION_FORMAT, FEEDBACK_LINK, SOLUTION_HIDDEN, TASK_TYPE
)
abstract class ChoiceTaskLocalMixin : LocalTaskMixin() {

  @JsonProperty
  private var isMultipleChoice: Boolean = false

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FeedbackCorrectFilter::class)
  @JsonProperty
  private lateinit var messageCorrect: String

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = FeedbackIncorrectFilter::class)
  @JsonProperty
  private lateinit var messageIncorrect: String

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = QuizHeaderFilter::class)
  @JsonProperty
  private lateinit var quizHeader: String
}

abstract class ChoiceOptionLocalMixin {
  @JsonProperty
  private var text: String = ""
}

@JsonPropertyOrder(NAME, IS_VISIBLE, TEXT, IS_BINARY, IS_EDITABLE, HIGHLIGHT_LEVEL)
@JsonDeserialize(builder = EduFileBuilder::class)
abstract class EduFileMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(IS_VISIBLE)
  var isVisible: Boolean = true

  lateinit var text: String
    @JsonProperty(TEXT)
    @Encrypt
    set

  var isBinary: Boolean? = null
    @JsonProperty(IS_BINARY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    get
    @JsonProperty(IS_BINARY)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    set

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(IS_EDITABLE)
  var isEditable: Boolean = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = HighlightLevelValueFilter::class)
  @JsonProperty(HIGHLIGHT_LEVEL)
  var errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
}

@Suppress("unused")
abstract class EduTestInfoMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  @JsonProperty(STATUS)
  private var status: Int = -1
}

class CourseDeserializer : StdDeserializer<Course>(Course::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): Course? {
    val node: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    return deserializeCourse(node, jp.codec)
  }

  private fun deserializeCourse(jsonObject: ObjectNode, codec: ObjectCodec): Course? {
    if (jsonObject.has(COURSE_TYPE)) {
      val course = codec.treeToValue(jsonObject, HyperskillCourse::class.java)
      return course
    }
    return codec.treeToValue(jsonObject, HyperskillCourse::class.java)
  }
}

private val LOG = logger<StudyItemDeserializer>()

class StudyItemDeserializer : StdDeserializer<StudyItem>(StudyItem::class.java) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext?): StudyItem? {
    val node: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    return deserializeItem(node, jp.codec)
  }

  private fun deserializeItem(jsonObject: ObjectNode, codec: ObjectCodec): StudyItem? {
    if (jsonObject.has(TASK_TYPE)) {
      val taskType = jsonObject.get(TASK_TYPE).asText()
      return deserializeTask(jsonObject, taskType, codec)
    }

    return if (!jsonObject.has(ITEM_TYPE)) {
      codec.treeToValue(jsonObject, Lesson::class.java)
    }
    else {
      val itemType = jsonObject.get(ITEM_TYPE).asText()
      when (itemType) {
        LESSON -> codec.treeToValue(jsonObject, Lesson::class.java)
        FRAMEWORK_TYPE -> codec.treeToValue(jsonObject, FrameworkLesson::class.java)
        SECTION -> codec.treeToValue(jsonObject, Section::class.java)
        else -> throw IllegalArgumentException("Unsupported item type: $itemType")
      }
    }
  }
}

fun deserializeTask(node: ObjectNode, taskType: String, objectMapper: ObjectCodec): Task? {
  return when (taskType) {
    IdeTask.IDE_TASK_TYPE -> objectMapper.treeToValue(node, IdeTask::class.java)
    TheoryTask.THEORY_TASK_TYPE -> objectMapper.treeToValue(node, TheoryTask::class.java)
    CodeTask.CODE_TASK_TYPE -> objectMapper.treeToValue(node, CodeTask::class.java)
    // deprecated: old courses have pycharm tasks
    EduTask.EDU_TASK_TYPE, EduTask.PYCHARM_TASK_TYPE -> {
      objectMapper.treeToValue(node, EduTask::class.java)
    }

    OutputTask.OUTPUT_TASK_TYPE -> objectMapper.treeToValue(node, OutputTask::class.java)
    RemoteEduTask.REMOTE_EDU_TASK_TYPE -> objectMapper.treeToValue(node, RemoteEduTask::class.java)
    UnsupportedTask.UNSUPPORTED_TASK_TYPE -> objectMapper.treeToValue(node, UnsupportedTask::class.java)
    else -> {
      LOG.warning("Unsupported task type $taskType")
      null
    }
  }
}

@JsonPOJOBuilder(withPrefix = "")
private open class EduFileBuilder {

  private var _name: String = ""
  var name: String
    @JsonProperty(NAME)
    set(value) {
      _name = value
    }
    @JsonProperty(NAME)
    get() = _name

  @JsonProperty(IS_VISIBLE)
  var isVisible: Boolean = true

  @JsonProperty(TEXT)
  @Encrypt
  var text: String? = null

  @JsonProperty(IS_BINARY)
  var isBinary: Boolean? = null

  @JsonProperty(IS_EDITABLE)
  var isEditable: Boolean = true

  @JsonProperty
  var isPropagatable: Boolean = true

  @JsonProperty(HIGHLIGHT_LEVEL)
  var errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION

  @JacksonInject(FILE_CONTENTS_FACTORY_INJECTABLE_VALUE)
  var fileContentsFactory: FileContentsFactory = EmtpyFileContentFactory

  private fun build(): EduFile {
    val result = EduFile()
    updateFile(result)

    return result
  }

  protected fun updateFile(result: EduFile) {
    result.name = name
    result.isVisible = isVisible
    result.isEditable = isEditable
    result.isPropagatable = isPropagatable
    result.errorHighlightLevel = errorHighlightLevel

    val text = this.text
    result.contents = if (text != null) {
      // The "text" field is not allowed starting from the 19th version of the format.
      // But we have this branch here because it is used when reading an older version of the course.json
      when (isBinary) {
        true -> InMemoryBinaryContents.parseBase64Encoding(text)
        false -> InMemoryTextualContents(text)
        null -> InMemoryUndeterminedContents(text)
      }
    }
    else {
      when (isBinary) {
        true -> fileContentsFactory.createBinaryContents(result)
        false -> fileContentsFactory.createTextualContents(result)
        null -> throw IllegalStateException("If the text field is absent, it must contain file binarity")
      }
    }
  }
}

@JsonPOJOBuilder(withPrefix = "")
private class TaskFileBuilder : EduFileBuilder() {

  private fun build(): TaskFile {
    val result = TaskFile()
    updateFile(result)

    return result
  }
}
