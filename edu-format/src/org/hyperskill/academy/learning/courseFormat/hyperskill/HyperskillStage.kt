package org.hyperskill.academy.learning.courseFormat.hyperskill

import com.fasterxml.jackson.annotation.JsonProperty
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.ID
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.IS_COMPLETED
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.STEP_ID
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.TITLE

class HyperskillStage {

  @Suppress("unused") //used for deserialization
  constructor()

  constructor(stageId: Int, stageTitle: String, stageStepId: Int, isStageCompleted: Boolean = false) {
    id = stageId
    title = stageTitle
    stepId = stageStepId
    isCompleted = isStageCompleted
  }

  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(STEP_ID)
  var stepId: Int = -1

  @JsonProperty(IS_COMPLETED)
  var isCompleted: Boolean = false
}
