package org.hyperskill.academy.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.annotation.JsonSetter
import org.hyperskill.academy.learning.courseFormat.CheckFeedback
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.json.mixins.NotImplementedInMixin
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.ACTUAL
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.EXPECTED
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FILES
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.MESSAGE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.RECORD
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.STATUS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TIME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE
import org.hyperskill.academy.learning.yaml.format.tasks.TaskYamlMixin
import java.util.*

@Suppress("UNUSED_PARAMETER", "unused") // used for yaml serialization
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, TAGS)
abstract class StudentTaskYamlMixin : TaskYamlMixin() {

  @get:JsonProperty(STATUS)
  @set:JsonProperty(STATUS)
  @get:JsonInclude(JsonInclude.Include.ALWAYS)
  protected override var status: CheckStatus = CheckStatus.Unchecked

  @get:JsonProperty(FEEDBACK)
  @set:JsonProperty(FEEDBACK)
  @get:JsonInclude(JsonInclude.Include.NON_NULL)
  protected override var feedback: CheckFeedback? = null

  // Don't serialize record - legacy field for old binary storage, no longer used
  // Keep setter for backwards compatibility when reading old YAML files
  @get:JsonIgnore
  @set:JsonSetter(RECORD)
  protected override var record: Int = -1
}

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(MESSAGE, TIME, EXPECTED, ACTUAL)
abstract class FeedbackYamlMixin {
  @JsonProperty(MESSAGE)
  @JsonInclude(JsonInclude.Include.ALWAYS)
  private var message: String = ""

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss zzz")
  @JsonProperty(TIME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var time: Date? = null

  @JsonProperty(EXPECTED)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var expected: String? = null

  @JsonProperty(ACTUAL)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var actual: String? = null
}

