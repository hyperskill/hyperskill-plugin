package com.jetbrains.edu.learning.courseFormat

import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Implementation of task file which contains task answer placeholders for student to type in and
 * which is visible to student in project view
 */
class TaskFile : EduFile {
  constructor()
  constructor(name: String, text: String) {
    this.name = name
    this.text = text
  }

  constructor(name: String, contents: FileContents) {
    this.name = name
    this.contents = contents
  }

  constructor(name: String, text: String, isVisible: Boolean) : this(name, text) {
    this.isVisible = isVisible
  }

  constructor(name: String, text: String, isVisible: Boolean, isLearnerCreated: Boolean) : this(name, text, isVisible) {
    this.isLearnerCreated = isLearnerCreated
  }

  @Transient
  private var _task: Task? = null

  var task: Task
    get() = _task ?: error("Task is null for TaskFile $name")
    set(value) {
      _task = value
    }

  fun initTaskFile(task: Task) {
    this.task = task
  }

  fun isValid(): Boolean {
    return true
  }
}