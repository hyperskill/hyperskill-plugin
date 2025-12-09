package org.hyperskill.academy.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.json.mixins.NotImplementedInMixin
import org.hyperskill.academy.learning.json.mixins.TrueValueFilter
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CONTENT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.IS_TEMPLATE_BASED
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = FrameworkLessonBuilder::class)
@JsonPropertyOrder(TYPE, CUSTOM_NAME, CONTENT, IS_TEMPLATE_BASED, TAGS)
abstract class FrameworkLessonYamlMixin : LessonYamlMixin() {

  @JsonInclude(JsonInclude.Include.CUSTOM, valueFilter = TrueValueFilter::class)
  @JsonProperty(IS_TEMPLATE_BASED)
  private val isTemplateBased: Boolean = true

  val itemType: String
    @JsonProperty(TYPE)
    get() = throw NotImplementedInMixin()
}

@JsonPOJOBuilder(withPrefix = "")
open class FrameworkLessonBuilder(
  @param:JsonProperty(IS_TEMPLATE_BASED) val isTemplateBased: Boolean = true,
  @JsonProperty(CONTENT) content: List<String?> = emptyList(),
  @JsonProperty(TAGS) contentTags: List<String> = emptyList(),
  @JsonProperty(CUSTOM_NAME) customName: String? = null
) : LessonBuilder(content, customName, contentTags) {
  override fun createLesson(): FrameworkLesson = FrameworkLesson().also {
    it.isTemplateBased = isTemplateBased
  }
}
