package org.hyperskill.academy.learning.courseFormat.hyperskill

import com.fasterxml.jackson.annotation.JsonProperty
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.ID
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.THEORY_ID
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.TITLE

class HyperskillTopic {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(THEORY_ID)
  var theoryId: Int? = null
}