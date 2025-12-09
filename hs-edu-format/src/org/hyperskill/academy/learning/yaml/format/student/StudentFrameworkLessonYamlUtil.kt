package org.hyperskill.academy.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.yaml.format.FrameworkLessonBuilder
import org.hyperskill.academy.learning.yaml.format.FrameworkLessonYamlMixin
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CONTENT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CURRENT_TASK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.IS_TEMPLATE_BASED
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = StudentFrameworkLessonBuilder::class)
@JsonPropertyOrder(TYPE, CUSTOM_NAME, CONTENT, IS_TEMPLATE_BASED, CURRENT_TASK, TAGS)
abstract class StudentFrameworkLessonYamlMixin : FrameworkLessonYamlMixin() {
  @JsonProperty(CURRENT_TASK)
  private var currentTaskIndex: Int = 0
}

private class StudentFrameworkLessonBuilder(
  @param:JsonProperty(CURRENT_TASK) val currentTaskIndex: Int,
  @JsonProperty(IS_TEMPLATE_BASED) isTemplateBased: Boolean = true,
  @JsonProperty(CONTENT) content: List<String?> = emptyList(),
  @JsonProperty(TAGS) contentTags: List<String> = emptyList(),
  @JsonProperty(CUSTOM_NAME) customName: String? = null
) : FrameworkLessonBuilder(isTemplateBased, content, contentTags = contentTags, customName = customName) {
  override fun createLesson(): FrameworkLesson {
    return super.createLesson().also { it.currentTaskIndex = currentTaskIndex }
  }
}