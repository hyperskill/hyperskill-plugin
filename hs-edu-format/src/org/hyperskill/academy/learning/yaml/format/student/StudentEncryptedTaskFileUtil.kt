package org.hyperskill.academy.learning.yaml.format.student

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import org.hyperskill.academy.learning.json.encrypt.Encrypt
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.EDITABLE
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.ENCRYPTED_TEXT
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.IS_BINARY
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.LEARNER_CREATED
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.NAME
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.PLACEHOLDERS
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.VISIBLE

@Suppress("unused") // used for yaml serialization
@JsonDeserialize(builder = StudentTaskFileBuilder::class)
@JsonPropertyOrder(NAME, VISIBLE, PLACEHOLDERS, EDITABLE, ENCRYPTED_TEXT, IS_BINARY, LEARNER_CREATED)
abstract class StudentEncryptedTaskFileYamlMixin : StudentTaskFileYamlMixin() {

  @JsonProperty(ENCRYPTED_TEXT)
  @Encrypt
  override fun getTextToSerialize(): String {
    throw NotImplementedError()
  }
}
