package com.jetbrains.edu.learning.stepik.api

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.serialization.SerializationUtils
import com.jetbrains.edu.learning.serialization.SerializationUtils.Json.NAME
import com.jetbrains.edu.learning.serialization.converter.json.*
import com.jetbrains.edu.learning.stepik.PyCharmStepOptions
import org.jetbrains.annotations.VisibleForTesting


class JacksonStepOptionsDeserializer(vc: Class<*>? = null) : StdDeserializer<PyCharmStepOptions>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): PyCharmStepOptions {
    val objectMapper = StepikBasedConnector.createObjectMapper(SimpleModule())
    val node: JsonNode = jp.codec.readTree(jp)
    val migratedNode = migrate(node as ObjectNode, JSON_FORMAT_VERSION)
    return objectMapper.treeToValue(migratedNode, PyCharmStepOptions::class.java)
  }

  companion object {
    @VisibleForTesting
    fun migrate(node: ObjectNode, maxVersion: Int): ObjectNode {
      var convertedStepOptions = node
      val versionJson = node.get(SerializationUtils.Json.FORMAT_VERSION)
      var version = 1
      if (versionJson != null) {
        version = versionJson.asInt()
      }
      while (version < maxVersion) {
        val converter = when (version) {
          1 -> ToSecondVersionJsonStepOptionsConverter()
          2 -> ToThirdVersionJsonStepOptionsConverter()
          3 -> ToFourthVersionJsonStepOptionsConverter()
          4 -> ToFifthVersionJsonStepOptionsConverter()
          5 -> ToSixthVersionJsonStepOptionConverter()
          6 -> ToSeventhVersionJsonStepOptionConverter()
          8 -> To9VersionJsonStepOptionConverter()
          9 -> To10VersionJsonStepOptionConverter()
          else -> null
        }
        if (converter != null) {
          convertedStepOptions = converter.convert(convertedStepOptions)
        }
        version++
      }
      node.put(SerializationUtils.Json.FORMAT_VERSION, maxVersion)
      return convertedStepOptions
    }
  }
}

class StepikReplyDeserializer(vc: Class<*>? = null) : StdDeserializer<Reply>(vc) {

  override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): Reply {
    val jsonObject: ObjectNode = jp.codec.readTree(jp) as ObjectNode
    val initialVersion = jsonObject.migrate(JSON_FORMAT_VERSION)

    val objectMapper = StepikBasedConnector.createObjectMapper(SimpleModule())
    val reply = objectMapper.treeToValue(jsonObject, Reply::class.java)
    // We need to save original version of reply object
    // to correct deserialize Reply#eduTask
    reply.version = initialVersion
    return reply
  }

  companion object {
    /**
     * Return object version before migration
     */
    @VisibleForTesting
    fun ObjectNode.migrate(maxVersion: Int): Int {
      val versionJson = get(SerializationUtils.Json.VERSION)
      if (versionJson == null && get(EDU_TASK) == null) {
        // solution doesn't contain any edu data, let's not migrate it
        return maxVersion
      }
      val initialVersion = versionJson?.asInt() ?: 1
      var version = initialVersion
      while (version < maxVersion) {
        when (version) {
          6 -> toSeventhVersion()
        }
        version++
      }
      put(SerializationUtils.Json.VERSION, maxVersion)
      return initialVersion
    }

    private fun ObjectNode.toSeventhVersion() {
      val solutionFiles = get("solution")
      if (solutionFiles == null) return
      if (solutionFiles.any { it.get(NAME).asText().endsWith(".py") }) return
      for (solutionFile in solutionFiles) {
        val value = solutionFile.get(NAME)?.asText()
        (solutionFile as ObjectNode).put(NAME, "src/$value")
      }
    }
  }
}
