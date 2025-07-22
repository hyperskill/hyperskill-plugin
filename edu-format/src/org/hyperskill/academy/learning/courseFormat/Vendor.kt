package org.hyperskill.academy.learning.courseFormat

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.EMAIL
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.NAME
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.URL


class Vendor {

  constructor()

  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(EMAIL)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var email: String? = null

  @JsonProperty(URL)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var url: String? = null

  override fun toString(): String = name
}
