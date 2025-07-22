package org.hyperskill.academy.learning.yaml.format.tasks

import org.hyperskill.academy.learning.courseFormat.tasks.Task

/**
 * Placeholder to deserialize task type, should be replaced with actual task later
 */
class TaskWithType(title: String) : Task(title) {
  override val itemType: String
    get() = throw NotImplementedError()
}