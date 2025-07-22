package org.hyperskill.academy.learning.courseFormat.hyperskill

import com.fasterxml.jackson.annotation.JsonProperty
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.DESCRIPTION
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.ENVIRONMENT
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.ID
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.IDE_FILES
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.IS_TEMPLATE_BASED
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.LANGUAGE
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.TITLE
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.USE_IDE

class HyperskillProject {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(DESCRIPTION)
  var description: String = ""

  @JsonProperty(IDE_FILES)
  var ideFiles: String = ""

  // some projects might not support IDE
  @JsonProperty(USE_IDE)
  var useIde: Boolean = true

  @JsonProperty(LANGUAGE)
  var language: String = ""

  @JsonProperty(ENVIRONMENT)
  var environment: String? = null

  @JsonProperty(IS_TEMPLATE_BASED)
  var isTemplateBased: Boolean = false
}
