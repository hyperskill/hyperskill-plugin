@file:Suppress("unused")

package org.hyperskill.academy.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseFormat.EduFileErrorHighlightLevel
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.message
import org.hyperskill.academy.learning.json.mixins.HighlightLevelValueFilter
import org.hyperskill.academy.learning.json.mixins.NotImplementedInMixin
import org.hyperskill.academy.learning.json.mixins.TrueValueFilter
import org.hyperskill.academy.learning.yaml.errorHandling.formatError
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.EDITABLE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.HIGHLIGHT_LEVEL
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.IS_BINARY
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.PROPAGATABLE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.VISIBLE
import org.hyperskill.academy.learning.yaml.format.student.IsBinaryFilter
import org.hyperskill.academy.learning.yaml.format.student.TakeFromStorageBinaryContents
import org.hyperskill.academy.learning.yaml.format.student.TakeFromStorageTextualContents

/**
 * Base mixin class used to deserialize task and additional files item.
 */
abstract class EduFileYamlMixin {
  @JsonProperty(NAME)
  private lateinit var name: String

  // Store additional unknown properties to preserve them during serialization
  @get:JsonAnyGetter
  @set:JsonAnySetter
  protected open var additionalProperties: MutableMap<String, Any?> = mutableMapOf()
}

@JsonDeserialize(builder = AdditionalFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, IS_BINARY)
abstract class AdditionalFileYamlMixin : EduFileYamlMixin() {

  @JsonProperty(VISIBLE)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  private var isVisible: Boolean = false

  private val isBinary: Boolean?
    @JsonProperty(IS_BINARY)
    @JsonInclude(value = JsonInclude.Include.CUSTOM, valueFilter = IsBinaryFilter::class)
    get() = throw NotImplementedInMixin()
}

/**
 * Mixin class is used to deserialize [TaskFile] item.
 * Update [TaskChangeApplier.applyTaskFileChanges] if new fields added to mixin
 */
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, HIGHLIGHT_LEVEL)
@JsonDeserialize(builder = TaskFileBuilder::class)
abstract class TaskFileYamlMixin : EduFileYamlMixin() {
  @JsonProperty(VISIBLE)
  private var isVisible = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(EDITABLE)
  private var isEditable = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(PROPAGATABLE)
  private val isPropagatable = true

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = HighlightLevelValueFilter::class)
  @JsonProperty(HIGHLIGHT_LEVEL)
  private var errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS
}

/**
 * A base class for building task files and additional files
 */
open class EduFileBuilder(
  @param:JsonProperty(NAME) val name: String?
) {
  protected open fun setupEduFile(eduFile: EduFile) {
    eduFile.name = name ?: formatError(message("yaml.editor.invalid.file.without.name"))
  }
}

@JsonPOJOBuilder(buildMethodName = "buildAdditionalFile", withPrefix = "")
class AdditionalFileBuilder(
  name: String?,
  @param:JsonProperty(VISIBLE) val isVisible: Boolean = false,
  @param:JsonProperty(IS_BINARY) val isBinary: Boolean? = false
) : EduFileBuilder(name) {

  fun buildAdditionalFile(): EduFile {
    val additionalFile = EduFile()
    setupEduFile(additionalFile)
    setupAdditionalFile(additionalFile)
    return additionalFile
  }

  private fun setupAdditionalFile(eduFile: EduFile) {
    eduFile.isVisible = isVisible
    eduFile.contents = if (isBinary == true) {
      TakeFromStorageBinaryContents
    }
    else {
      TakeFromStorageTextualContents
    }
  }
}

@JsonPOJOBuilder(buildMethodName = "buildTaskFile", withPrefix = "")
open class TaskFileBuilder(
  name: String?,
  @param:JsonProperty(VISIBLE) val visible: Boolean = true,
  @param:JsonProperty(EDITABLE) val editable: Boolean = true,
  @param:JsonProperty(PROPAGATABLE) val propagatable: Boolean = true,
  @param:JsonProperty(HIGHLIGHT_LEVEL) val errorHighlightLevel: EduFileErrorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS
) : EduFileBuilder(name) {

  @Suppress("unused") //used for deserialization
  fun buildTaskFile(): TaskFile {
    val taskFile = TaskFile()
    setupEduFile(taskFile)
    setupTaskFile(taskFile)
    return taskFile
  }

  protected open fun setupTaskFile(taskFile: TaskFile) {
    taskFile.isVisible = visible
    taskFile.isEditable = editable
    taskFile.isPropagatable = propagatable
    if (errorHighlightLevel != EduFileErrorHighlightLevel.ALL_PROBLEMS && errorHighlightLevel != EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION) {
      taskFile.errorHighlightLevel = errorHighlightLevel
    }
  }
}