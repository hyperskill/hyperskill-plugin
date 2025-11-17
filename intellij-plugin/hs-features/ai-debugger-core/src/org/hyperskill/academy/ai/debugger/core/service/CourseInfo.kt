package org.hyperskill.academy.ai.debugger.core.service

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty

enum class CourseType(val value: String) {
  HYPERSKILL("Hyperskill");
}

data class CourseInfo(
  @field:JsonProperty(value = "id")
  val id: Int,

  @field:JsonProperty(value = "type")
  val type: CourseType = CourseType.HYPERSKILL,

  @field:JsonProperty(value = "update_version")
  @field:JsonInclude(JsonInclude.Include.NON_NULL)
  val updateVersion: Int? = null,
)