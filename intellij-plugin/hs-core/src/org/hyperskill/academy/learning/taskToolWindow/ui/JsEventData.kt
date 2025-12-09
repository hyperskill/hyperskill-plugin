package org.hyperskill.academy.learning.taskToolWindow.ui

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.util.StdConverter
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.intellij.openapi.diagnostic.Logger

private const val TERM = "term"
private const val X = "x"
private const val Y = "y"
private const val BOTTOM = "bottomOfTermRect"
private const val TOP = "topOfTermRect"

data class JsEventData(
  @param:JsonProperty(TERM)
  val term: String,

  @param:JsonProperty(X)
  @param:JsonDeserialize(contentConverter = DoubleToIntConverter::class)
  val x: Int,

  @param:JsonProperty(Y)
  @param:JsonDeserialize(contentConverter = DoubleToIntConverter::class)
  val y: Int,

  @param:JsonProperty(BOTTOM)
  @param:JsonDeserialize(contentConverter = DoubleToIntConverter::class)
  val bottomOfTermRect: Int? = null,

  @param:JsonProperty(TOP)
  @param:JsonDeserialize(contentConverter = DoubleToIntConverter::class)
  val topOfTermRect: Int? = null
) {
  companion object {
    private val LOG: Logger = Logger.getInstance(JsEventData::class.java)

    fun fromJson(json: String): JsEventData? =
      runCatching {
        jacksonObjectMapper().readValue(json, JsEventData::class.java)
      }.onFailure {
        LOG.error("Failed to parse js event data $json", it)
      }.getOrNull()

    private class DoubleToIntConverter : StdConverter<Double, Int>() {
      override fun convert(value: Double): Int = value.toInt()
    }
  }
}
