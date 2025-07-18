package com.jetbrains.edu.learning.yaml.format

import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.courseFormat.tasks.TheoryTask
import com.jetbrains.edu.learning.yaml.YamlLoader.addItemAsNew
import org.jetbrains.annotations.NonNls

open class TaskChangeApplier(val project: Project) : StudyItemChangeApplier<Task>() {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    @NonNls
    val messageToLog = "Project not found for ${existingItem.name}"
    val project = existingItem.project ?: error(messageToLog)
    if (existingItem.itemType != deserializedItem.itemType) {
      changeType(project, existingItem, deserializedItem)
      return
    }
    existingItem.feedbackLink = deserializedItem.feedbackLink
    @Suppress("DEPRECATION") // it's ok as we just copy value of deprecated field
    existingItem.customPresentableName = deserializedItem.customPresentableName
    existingItem.contentTags = deserializedItem.contentTags
    existingItem.solutionHidden = deserializedItem.solutionHidden
    if (deserializedItem is TheoryTask && existingItem is TheoryTask) {
      existingItem.postSubmissionOnOpen = deserializedItem.postSubmissionOnOpen
    }
    existingItem.applyTaskFileChanges(deserializedItem)
  }

  open fun changeType(project: Project, existingItem: StudyItem, deserializedItem: Task) {
    val existingTask = existingItem as Task

    deserializedItem.name = existingItem.name
    deserializedItem.index = existingItem.index

    val parentItem = existingTask.lesson
    parentItem.removeItem(existingItem)
    parentItem.addItemAsNew(project, deserializedItem)
  }

  private fun Task.applyTaskFileChanges(deserializedItem: Task) {
    val orderedTaskFiles = LinkedHashMap<String, TaskFile>()
    for ((name, deserializedTaskFile) in deserializedItem.taskFiles) {
      val existingTaskFile = taskFiles[name]
      val taskFile: TaskFile = if (existingTaskFile != null) {
        applyTaskFileChanges(existingTaskFile, deserializedTaskFile)
        existingTaskFile
      }
      else {
        deserializedTaskFile
      }
      orderedTaskFiles[name] = taskFile
      deserializedTaskFile.initTaskFile(this)
    }
    taskFiles = orderedTaskFiles
  }

  protected open fun applyTaskFileChanges(
    existingTaskFile: TaskFile,
    deserializedTaskFile: TaskFile
  ) {
    existingTaskFile.isVisible = deserializedTaskFile.isVisible
    existingTaskFile.isEditable = deserializedTaskFile.isEditable
    existingTaskFile.isPropagatable = deserializedTaskFile.isPropagatable
    existingTaskFile.errorHighlightLevel = deserializedTaskFile.errorHighlightLevel
  }

}