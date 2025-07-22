package org.hyperskill.academy.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.util.StdConverter
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.yaml.format.CourseYamlMixin
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.ADDITIONAL_FILES
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CONTENT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_CONTENT_PATH
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.ENVIRONMENT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.LANGUAGE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.MODE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.PROGRAMMING_LANGUAGE_VERSION
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.SUMMARY
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TITLE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE

@Suppress("unused") // used for yaml serialization
@JsonPropertyOrder(
  TYPE,
  TITLE,
  LANGUAGE,
  SUMMARY,
  PROGRAMMING_LANGUAGE,
  PROGRAMMING_LANGUAGE_VERSION,
  ENVIRONMENT,
  CONTENT,
  CUSTOM_CONTENT_PATH,
  ADDITIONAL_FILES,
  MODE,
  TAGS
)
abstract class StudentCourseYamlMixin : CourseYamlMixin() {
  @JsonSerialize(converter = CourseModeSerializationConverter::class)
  @JsonProperty(MODE)
  private var courseMode = CourseMode.STUDENT
}

private class CourseModeSerializationConverter : StdConverter<CourseMode, String>() {
  override fun convert(courseMode: CourseMode): String {
    return courseMode.toString()
  }
}
