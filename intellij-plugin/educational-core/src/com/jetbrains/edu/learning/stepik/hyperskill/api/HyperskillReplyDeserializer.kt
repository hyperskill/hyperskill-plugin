package com.jetbrains.edu.learning.stepik.hyperskill.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.diagnostic.Logger
import com.jetbrains.edu.learning.stepik.api.*
import com.jetbrains.rd.util.first

class HyperskillReplyDeserializer(vc: Class<*>? = null) : StdDeserializer<Reply>(vc) {
  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Reply {
    val jsonObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    val type = jsonObject.tryGuessType()
    val objectMapper = StepikBasedConnector.createObjectMapper(SimpleModule())
    return objectMapper.treeToValue(jsonObject, type)
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(StepikReplyDeserializer::class.java)

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
        else {
          // Don't want to log this as an error because this is the way for submissions of unsupported hyperskill tasks
          LOG.warn("Could not guess type of reply: cannot be mapped to an already existing type.")
        }
        return Reply::class.java
      }

      val (type, _) = possibleTypes.first()
      return type
    }
  }
}
