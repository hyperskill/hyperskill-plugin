@file:Suppress("unused")

package org.hyperskill.academy.learning.yaml.format.tasks

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FILES
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.POST_SUBMISSION_ON_OPEN
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.RECORD
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.STATUS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE
import org.hyperskill.academy.learning.yaml.format.student.StudentTaskYamlMixin

@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, STATUS, FEEDBACK, RECORD, TAGS)
abstract class TheoryTaskYamlUtil : StudentTaskYamlMixin() {
  @JsonProperty(POST_SUBMISSION_ON_OPEN)
  private var postSubmissionOnOpen: Boolean = true
}