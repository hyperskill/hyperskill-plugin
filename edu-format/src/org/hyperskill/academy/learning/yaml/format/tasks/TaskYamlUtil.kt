@file:JvmName("TaskYamlUtil")

package org.hyperskill.academy.learning.yaml.format.tasks

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.json.mixins.NotImplementedInMixin
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FILES
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.SOLUTION_HIDDEN
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE

/**
 * Mixin class is used to deserialize [Task] item.
 * Update [org.hyperskill.academy.learning.yaml.format.TaskChangeApplier] if new fields added to mixin
 */
@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, SOLUTION_HIDDEN, TAGS)
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

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  protected open lateinit var contentTags: List<String>
}
