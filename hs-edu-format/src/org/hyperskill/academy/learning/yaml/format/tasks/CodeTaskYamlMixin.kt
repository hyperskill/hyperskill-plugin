package org.hyperskill.academy.learning.yaml.format.tasks

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.SUBMISSION_LANGUAGE
import org.hyperskill.academy.learning.yaml.format.student.StudentTaskYamlMixin

@Suppress("unused") // used for yaml serialization
class CodeTaskYamlMixin : StudentTaskYamlMixin() {

  @JsonProperty(SUBMISSION_LANGUAGE)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  private var submissionLanguage: String? = null

}
