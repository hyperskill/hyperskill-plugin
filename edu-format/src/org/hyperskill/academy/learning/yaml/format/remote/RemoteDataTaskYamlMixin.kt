@file:Suppress("unused") // used for yaml serialization
package org.hyperskill.academy.learning.yaml.format.remote

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.ATTEMPT
import org.hyperskill.academy.learning.courseFormat.attempts.DataTaskAttempt
import org.hyperskill.academy.learning.json.mixins.NotImplementedInMixin
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.END_DATE_TIME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.ID
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.UPDATE_DATE
import java.util.*

@JsonPropertyOrder(TYPE, ID, UPDATE_DATE, ATTEMPT)
abstract class RemoteDataTaskYamlMixin : RemoteStudyItemYamlMixin() {
  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()

  @JsonProperty(ATTEMPT)
  private var attempt: DataTaskAttempt? = null
}

@JsonPropertyOrder(ID, END_DATE_TIME)
abstract class DataTaskAttemptYamlMixin {
  @JsonProperty(ID)
  private var id: Int = -1

  @JsonIgnore
  private lateinit var time: Date

  @JsonIgnore
  private var timeLeft: Long = 0

  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss zzz")
  @JsonProperty(END_DATE_TIME)
  private lateinit var endDateTime: Date
}
