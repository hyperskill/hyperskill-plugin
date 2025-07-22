@file:JvmName("LessonYamlUtil")
@file:Suppress("unused")

package org.hyperskill.academy.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.yaml.errorHandling.formatError
import org.hyperskill.academy.learning.yaml.errorHandling.unnamedItemAtMessage
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CONTENT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.tasks.TaskWithType

/**
 * Mixin class is used to deserialize [Lesson] item.
 * Update [ItemContainerChangeApplier] if new fields added to mixin
 */
@JsonPropertyOrder(CUSTOM_NAME, CONTENT, TAGS)
@JsonDeserialize(builder = LessonBuilder::class)
abstract class LessonYamlMixin {
  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  @JsonProperty(CONTENT)
  @JsonSerialize(contentConverter = StudyItemConverter::class)
  private lateinit var _items: List<StudyItem>

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  protected open lateinit var contentTags: List<String>
}

@JsonPOJOBuilder(withPrefix = "")
open class LessonBuilder(
  @JsonProperty(CONTENT) val content: List<String?> = emptyList(),
  @JsonProperty(CUSTOM_NAME) val customName: String? = null,
  @JsonProperty(TAGS) val contentTags: List<String> = emptyList()
) {
  private fun build(): Lesson {
    val lesson = createLesson()
    val taskList = content.mapIndexed { index: Int, title: String? ->
      if (title == null) {
        formatError(unnamedItemAtMessage(index + 1))
      }
      val task = TaskWithType(title)
      task.index = index + 1
      task
    }

    lesson.items = taskList
    lesson.customPresentableName = customName
    lesson.contentTags = contentTags
    return lesson
  }

  protected open fun createLesson(): Lesson = Lesson()
}
