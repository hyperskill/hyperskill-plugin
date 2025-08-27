package org.hyperskill.academy.learning.yaml.format.remote

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.yaml.errorHandling.formatError
import org.hyperskill.academy.learning.yaml.errorHandling.unsupportedItemTypeMessage
import org.hyperskill.academy.learning.yaml.format.CourseBuilder
import org.hyperskill.academy.learning.yaml.format.CourseYamlMixin
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CONTENT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_CONTENT_PATH
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.HYPERSKILL_TYPE_YAML
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.LANGUAGE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.SUMMARY
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TITLE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = RemoteCourseBuilder::class)
abstract class RemoteCourseYamlMixin : CourseYamlMixin()

@JsonPOJOBuilder(withPrefix = "")
class RemoteCourseBuilder(
  @JsonProperty(TYPE) courseType: String?,
  @JsonProperty(TITLE) title: String,
  @JsonProperty(SUMMARY) summary: String,
  @JsonProperty(PROGRAMMING_LANGUAGE) displayProgrammingLanguageName: String,
  @JsonProperty(PROGRAMMING_LANGUAGE_VERSION) programmingLanguageVersion: String?,
  @JsonProperty(LANGUAGE) language: String,
  @JsonProperty(ENVIRONMENT) yamlEnvironment: String?,
  @JsonProperty(CONTENT) content: List<String?> = emptyList(),
  @JsonProperty(SOLUTIONS_HIDDEN) areSolutionsHidden: Boolean?,
  @JsonProperty(TAGS) yamlContentTags: List<String> = emptyList(),
  @JsonProperty(CUSTOM_CONTENT_PATH) customContentPath: String = "",
) : CourseBuilder(
  courseType,
  title,
  summary,
  displayProgrammingLanguageName,
  programmingLanguageVersion,
  language,
  yamlEnvironment,
  content,
  areSolutionsHidden,
  yamlContentTags,
  pathToContent = customContentPath,
) {

  override fun makeCourse(): Course {
    return when (courseType) {
      HYPERSKILL_TYPE_YAML -> HyperskillCourse()
      else -> formatError(unsupportedItemTypeMessage(courseType ?: "Unknown", EduFormatNames.COURSE))
    }
  }
}
