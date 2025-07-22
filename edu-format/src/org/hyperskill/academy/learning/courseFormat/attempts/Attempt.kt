package org.hyperskill.academy.learning.courseFormat.attempts

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.DATASET
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.STATUS
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.STEP
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.USER
import org.jetbrains.annotations.TestOnly
import java.util.*

class Attempt : AttemptBase {
  @JsonProperty(STEP)
  var step: Int = 0

  @JsonProperty(DATASET)
  var dataset: Dataset? = null

  @JsonProperty(STATUS)
  var status: String? = null

  @JsonProperty(USER)
  var user: String? = null

  val isActive: Boolean
    @JsonIgnore
    get() = status == "active"

  constructor()

  constructor(step: Int) {
    this.step = step
  }

  @TestOnly
  constructor(id: Int, time: Date, timeLeft: Int) : super(id, time, timeLeft.toLong())
}