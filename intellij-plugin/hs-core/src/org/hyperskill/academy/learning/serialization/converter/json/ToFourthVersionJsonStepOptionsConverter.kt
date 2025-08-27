package org.hyperskill.academy.learning.serialization.converter.json

import com.fasterxml.jackson.databind.node.ObjectNode
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.serialization.SerializationUtils
import org.hyperskill.academy.learning.stepik.StepikNames

class ToFourthVersionJsonStepOptionsConverter : JsonStepOptionsConverter {
  override fun convert(stepOptionsJson: ObjectNode): ObjectNode {
    if (stepOptionsJson.has(SerializationUtils.Json.TITLE)
        && StepikNames.PYCHARM_ADDITIONAL == stepOptionsJson[SerializationUtils.Json.TITLE].asText()
    ) {
      stepOptionsJson.remove(SerializationUtils.Json.TITLE)
      stepOptionsJson.put(SerializationUtils.Json.TITLE, EduNames.ADDITIONAL_MATERIALS)
    }
    return stepOptionsJson
  }
}
