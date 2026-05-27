package org.hyperskill.academy.learning.yaml.format.student

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.yaml.errorHandling.YamlLoadingException
import org.hyperskill.academy.learning.yaml.format.TaskChangeApplier

class StudentTaskChangeApplier(project: Project) : TaskChangeApplier(project) {
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    if (existingItem.solutionHidden != deserializedItem.solutionHidden && !ApplicationManager.getApplication().isInternal) {
      throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.visibility.cannot.be.changed"))
    }
    super.applyChanges(existingItem, deserializedItem)

    // Apply status and feedback from deserialized item
    existingItem.status = deserializedItem.status
    existingItem.feedback = deserializedItem.feedback
    // `record` is a legacy framework-lesson storage pointer. YAML reloads can deserialize
    // a default -1 and must not wipe a live in-memory record before migration reads it.
    if (deserializedItem.record != -1) {
      existingItem.record = deserializedItem.record
    }

    if (existingItem is RemoteEduTask && deserializedItem is RemoteEduTask) {
      val newCheckProfile = deserializedItem.checkProfile
      if (newCheckProfile.isNotEmpty() && newCheckProfile != existingItem.checkProfile) {
        existingItem.checkProfile = newCheckProfile
      }
    }
  }

  override fun applyTaskFileChanges(existingTaskFile: TaskFile, deserializedTaskFile: TaskFile) {
    super.applyTaskFileChanges(existingTaskFile, deserializedTaskFile)
    existingTaskFile.contents = deserializedTaskFile.contents
  }

  override fun changeType(project: Project, existingItem: StudyItem, deserializedItem: Task) {
    throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.not.allowed.to.change.task"))
  }
}
