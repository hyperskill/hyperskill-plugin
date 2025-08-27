package org.hyperskill.academy.learning.yaml.format.remote

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.CHECK_PROFILE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.CUSTOM_NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FEEDBACK_LINK
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.FILES
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.RECORD
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.STATUS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TAGS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TYPE
import org.hyperskill.academy.learning.yaml.format.student.StudentTaskYamlMixin

@Suppress("unused")
@JsonPropertyOrder(TYPE, CUSTOM_NAME, FILES, FEEDBACK_LINK, CHECK_PROFILE, STATUS, FEEDBACK, RECORD, TAGS)
class RemoteEduTaskYamlMixin : StudentTaskYamlMixin() {
  @JsonProperty(CHECK_PROFILE)
  private lateinit var checkProfile: String
}