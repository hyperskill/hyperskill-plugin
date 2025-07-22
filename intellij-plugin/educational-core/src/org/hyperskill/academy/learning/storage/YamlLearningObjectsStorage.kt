package org.hyperskill.academy.learning.storage

class YamlLearningObjectsStorage : InMemoryLearningObjectsStorage() {
  override val writeTextInYaml = true
}