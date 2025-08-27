package org.hyperskill.academy.learning.yaml.format.student

import com.fasterxml.jackson.annotation.*
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
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, RECORD, TAGS)
abstract class StudentTaskYamlMixin : TaskYamlMixin() {

  protected var checkStatus: CheckStatus
    @JsonGetter(STATUS)
    get() = throw NotImplementedInMixin()
    @JsonSetter(STATUS)
    set(value) {
      throw NotImplementedInMixin()
    }

  @JsonProperty(FEEDBACK)
  private lateinit var feedback: CheckFeedback

  @JsonProperty(RECORD)
  protected open var record: Int = -1
}

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(MESSAGE, TIME, EXPECTED, ACTUAL)
abstract class FeedbackYamlMixin {
  @JsonProperty(MESSAGE)
  private var message: String = ""

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss zzz")
  @JsonProperty(TIME)
  private var time: Date? = null

  @JsonProperty(EXPECTED)
  private var expected: String? = null

  @JsonProperty(ACTUAL)
  private var actual: String? = null
}

