@file:JvmName("SectionYamlUtil")

package org.hyperskill.academy.learning.yaml.format

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.json.mixins.NotImplementedInMixin
import org.hyperskill.academy.learning.yaml.errorHandling.formatError
import org.hyperskill.academy.learning.yaml.errorHandling.unnamedItemAtMessage
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CONTENT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS

/**
 * Mixin class is used to deserialize [Section] item.
 * Update [ItemContainerChangeApplier] if new fields added to mixin
 */
@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(CUSTOM_NAME, CONTENT, TAGS)
@JsonDeserialize(builder = SectionBuilder::class)
abstract class SectionYamlMixin {
  @JsonProperty(CUSTOM_NAME)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var customPresentableName: String? = null

  // Not the `items` themselves: the names also keep the entries that could not be resolved on load,
  // see `ItemContainer.unresolvedContent`
  val contentNames: List<String>
    @JsonProperty(CONTENT)
    get() = throw NotImplementedInMixin()

  @JsonProperty(TAGS)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  private lateinit var contentTags: List<String>
}

@JsonPOJOBuilder(withPrefix = "")
private class SectionBuilder(
  @param:JsonProperty(CONTENT) val content: List<String?> = emptyList(),
  @param:JsonProperty(CUSTOM_NAME) val customName: String? = null,
  @param:JsonProperty(TAGS) val contentTags: List<String> = emptyList()
) {
  @Suppress("unused") //used for deserialization
  private fun build(): Section {
    val section = Section()
    val items = content.mapIndexed { index: Int, title: String? ->
      if (title == null) {
        formatError(unnamedItemAtMessage(index + 1))
      }
      val titledStudyItem = TitledStudyItem(title)
      titledStudyItem.index = index + 1
      titledStudyItem
    }
    section.items = items
    section.customPresentableName = customName
    section.contentTags = contentTags
    return section
  }
}
