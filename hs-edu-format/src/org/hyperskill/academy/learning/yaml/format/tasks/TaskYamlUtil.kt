@file:JvmName("TaskYamlUtil")

package org.hyperskill.academy.learning.yaml.format.tasks

import com.fasterxml.jackson.annotation.JsonAnyGetter
import com.fasterxml.jackson.annotation.JsonAnySetter
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.hyperskill.academy.learning.courseFormat.CheckFeedback
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.json.mixins.NotImplementedInMixin
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FILES
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.RECORD
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.SOLUTION_HIDDEN
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.STATUS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE

/**
 * Mixin class is used to deserialize [Task] item.
 * Update [org.hyperskill.academy.learning.yaml.format.TaskChangeApplier] if new fields added to mixin
 */
@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, SOLUTION_HIDDEN, STATUS, FEEDBACK, RECORD, TAGS)
abstract class TaskYamlMixin {
  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(FILES)
  open fun getTaskFileValues(): Collection<TaskFile> {
    throw NotImplementedInMixin()
  }

  @JsonProperty(FILES)
  open fun setTaskFileValues(taskFiles: List<TaskFile>) {
    throw NotImplementedInMixin()
  }

  @JsonProperty(value = FEEDBACK_LINK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected open lateinit var feedbackLink: String

  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(SOLUTION_HIDDEN)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var solutionHidden: Boolean? = null

  @JsonProperty(STATUS)
  @JsonInclude(JsonInclude.Include.ALWAYS)
  protected open var status: CheckStatus = CheckStatus.Unchecked

  @JsonProperty(FEEDBACK)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  protected open var feedback: CheckFeedback? = null

  @JsonProperty(RECORD)
  @JsonInclude(JsonInclude.Include.NON_DEFAULT)
  protected open var record: Int = -1

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  protected open lateinit var contentTags: List<String>

  // Store additional unknown properties to preserve them during serialization
  @JsonIgnore
  protected open lateinit var additionalProperties: MutableMap<String, Any?>

  // The getter filters out known fields to avoid duplication
  @JsonAnyGetter
  protected open fun getAdditionalPropertiesForSerialization(): Map<String, Any?> {
    // Filter out known field names that are already serialized by the mixin
    val knownFields = setOf("type", "custom_name", "files", "feedback_link",
      "solution_hidden", "status", "feedback", "record", "tags")
    return additionalProperties.filterKeys { it !in knownFields }
  }

  @JsonAnySetter
  protected open fun setAdditionalProperty(key: String, value: Any?) {
    additionalProperties[key] = value
  }
}
