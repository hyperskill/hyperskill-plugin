package org.hyperskill.academy.learning.courseFormat

import org.hyperskill.academy.learning.courseFormat.EduFormatNames.FRAMEWORK
import org.hyperskill.academy.learning.courseFormat.tasks.Task

class FrameworkLesson() : Lesson() {

  constructor(lesson: Lesson) : this() {
    id = lesson.id
    updateDate = lesson.updateDate
    name = lesson.name
    items = lesson.items
    parent = lesson.parent
    index = lesson.index
    @Suppress("DEPRECATION")
    customPresentableName = lesson.customPresentableName
  }

  var currentTaskIndex: Int = 0
  var isTemplateBased: Boolean = true

  /**
   * currentTask is null when lesson is empty
   */
  fun currentTask(): Task? = taskList.getOrNull(currentTaskIndex)

  override val itemType: String = FRAMEWORK
}
