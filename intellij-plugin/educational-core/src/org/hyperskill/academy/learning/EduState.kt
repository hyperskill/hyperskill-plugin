package org.hyperskill.academy.learning

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task


data class EduState(
  val project: Project,
  val virtualFile: VirtualFile,
  val editor: Editor,
  val taskFile: TaskFile,
  val task: Task = taskFile.task,
)