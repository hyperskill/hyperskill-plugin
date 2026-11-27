package org.hyperskill.academy.learning.stepik.hyperskill.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.rd.util.first
import org.hyperskill.academy.learning.stepik.api.*

class HyperskillReplyDeserializer(vc: Class<*>? = null) : StdDeserializer<Reply>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Reply {
    val jsonObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    val type = jsonObject.tryGuessType()
    val objectMapper = StepikBasedConnector.createObjectMapper(SimpleModule())
    return objectMapper.treeToValue(jsonObject, type)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(HyperskillReplyDeserializer::class.java)

    private fun ObjectNode.tryGuessType(): Class<out Reply> {
      val typesToImportantField = mapOf(
        CodeTaskReply::class.java to CODE,
        EduTaskReply::class.java to SOLUTION,
      )
      val possibleTypes = typesToImportantField.filter { get(it.value)?.isNull == false }

      if (possibleTypes.size != 1) {
        if (possibleTypes.size > 1) {
          LOG.error("Could not guess type of reply: the choice of type is ambiguous.")
        }
        // Everything except code and files is a quiz reply: its fields differ between block types, and there is no need to tell them apart
        return QuizReply::class.java
      }

      val (type, _) = possibleTypes.first()
      return type
    }
  }
}
