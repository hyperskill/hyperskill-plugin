@file:Suppress("unused")

package org.hyperskill.academy.learning.yaml.format.hyperskill

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.ID
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.IDE_FILES
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.IS_TEMPLATE_BASED
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.STEP_ID
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.TITLE
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.TOPICS
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.USE_IDE
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTopic
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.UPDATE_DATE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.HYPERSKILL_PROJECT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.STAGES
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.THEORY_ID
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@JsonPropertyOrder(HYPERSKILL_PROJECT, UPDATE_DATE, STAGES, TOPICS)
abstract class HyperskillCourseMixin {
  @JsonProperty(HYPERSKILL_PROJECT)
  lateinit var hyperskillProject: HyperskillProject

  @Suppress("unused")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "EEE, dd MMM yyyy HH:mm:ss zzz")
  @JsonProperty(UPDATE_DATE)
  private lateinit var updateDate: Date

  @JsonProperty(STAGES)
  var stages: List<HyperskillStage> = mutableListOf()

  @JsonProperty(TOPICS)
  var taskToTopics: MutableMap<Int, List<HyperskillTopic>> = ConcurrentHashMap()
}

@Suppress("unused")
@JsonPropertyOrder(ID, IDE_FILES, IS_TEMPLATE_BASED, USE_IDE)
abstract class HyperskillProjectMixin {
  @JsonIgnore
  private var myId: Int = 0

  @JsonIgnore
  private lateinit var updateDate: Date

  @JsonProperty(ID)
  var id: Int = -1

  @JsonIgnore
  var title: String = ""

  @JsonIgnore
  var description: String = ""

  @JsonProperty(IDE_FILES)
  var ideFiles: String = ""

  @JsonProperty(USE_IDE)
  var useIde: Boolean = false

  @JsonIgnore
  var language: String = ""

  @JsonProperty(IS_TEMPLATE_BASED)
  var isTemplateBased: Boolean = false
}

@JsonPropertyOrder(ID, STEP_ID)
class HyperskillStageMixin {
  @JsonProperty(ID)
  var id: Int = -1

  @JsonIgnore
  var title: String = ""

  @JsonProperty(STEP_ID)
  var stepId: Int = -1
}

@JsonPropertyOrder(TITLE, THEORY_ID)
class HyperskillTopicMixin {
  @JsonIgnore
  var id: Int = -1

  @JsonProperty(TITLE)
  var title: String = ""

  @JsonProperty(THEORY_ID)
  var theoryId: Int? = null
}
