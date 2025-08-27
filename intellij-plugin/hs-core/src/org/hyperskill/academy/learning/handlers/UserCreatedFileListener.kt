package org.hyperskill.academy.learning.handlers

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.FileInfo
import org.hyperskill.academy.learning.courseFormat.TaskFile

class UserCreatedFileListener(project: Project) : EduVirtualFileListener(project) {

  override fun fileDeleted(fileInfo: FileInfo, file: VirtualFile) {
    val (task, pathInTask) = fileInfo as? FileInfo.FileInTask ?: return
    task.removeTaskFile(pathInTask)
  }

  override fun taskFileCreated(taskFile: TaskFile, file: VirtualFile) {
    super.taskFileCreated(taskFile, file)
    taskFile.isLearnerCreated = true
  }
}
