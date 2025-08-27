package org.hyperskill.academy.learning.courseFormat.attempts

import com.fasterxml.jackson.annotation.JsonProperty
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.COLUMNS
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.IS_CHECKBOX
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.IS_MULTIPLE_CHOICE
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.OPTIONS
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PAIRS
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.ROWS

@Suppress("unused", "UNUSED_PARAMETER")
class Dataset {
  @JsonProperty(IS_MULTIPLE_CHOICE)
  var isMultipleChoice: Boolean = false

  @JsonProperty(OPTIONS)
  var options: List<String>? = null

  @JsonProperty(PAIRS)
  var pairs: List<Pair>? = null

  @JsonProperty(ROWS)
  var rows: List<String>? = null

  @JsonProperty(COLUMNS)
  var columns: List<String>? = null

  @JsonProperty(IS_CHECKBOX)
  var isCheckbox: Boolean = false

  constructor()
  constructor(emptyDataset: String)  // stepik returns empty string instead of null

  class Pair {
    @JsonProperty("first")
    val first: String = ""

    @JsonProperty("second")
    val second: String = ""
  }
}
