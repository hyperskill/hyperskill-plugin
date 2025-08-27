package org.hyperskill.academy.shell

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

class ShellTaskCheckerProvider : TaskCheckerProvider {
  // All Shell Script tasks are remote ones, so only remote checking will be performed
  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask>? = null
}