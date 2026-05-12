package org.hyperskill.academy.learning.yaml.format.student

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.yaml.errorHandling.YamlLoadingException
import org.hyperskill.academy.learning.yaml.format.TaskChangeApplier

class StudentTaskChangeApplier(project: Project) : TaskChangeApplier(project) {
  private val LOG = logger<StudentTaskChangeApplier>()
  override fun applyChanges(existingItem: Task, deserializedItem: Task) {
    LOG.info("Applying changes for task: ${existingItem.name} (id=${existingItem.id})")
    LOG.info("Existing item checkProfile: ${(existingItem as? RemoteEduTask)?.checkProfile}")
    LOG.info("Deserialized item checkProfile: ${(deserializedItem as? RemoteEduTask)?.checkProfile}")
    if (existingItem.solutionHidden != deserializedItem.solutionHidden && !ApplicationManager.getApplication().isInternal) {
      throw YamlLoadingException(EduCoreBundle.message("yaml.editor.invalid.visibility.cannot.be.changed"))
    }
    super.applyChanges(existingItem, deserializedItem)

    // Apply status and feedback from deserialized item
    existingItem.status = deserializedItem.status
    existingItem.feedback = deserializedItem.feedback
    // Note: record is no longer serialized (legacy field), don't overwrite existing value

    if (existingItem is RemoteEduTask && deserializedItem is RemoteEduTask) {
      val newCheckProfile = deserializedItem.checkProfile
      if (newCheckProfile != existingItem.checkProfile) {
        LOG.info("Updating checkProfile for task ${existingItem.name}: '${existingItem.checkProfile}' -> '$newCheckProfile'")
        existingItem.checkProfile = newCheckProfile
      }
      if (existingItem.checkProfile.isEmpty()) {
        LOG.warn("checkProfile is empty for RemoteEduTask ${existingItem.name} after applying changes")
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