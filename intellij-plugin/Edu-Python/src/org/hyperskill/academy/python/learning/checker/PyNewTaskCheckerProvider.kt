package org.hyperskill.academy.python.learning.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.CodeExecutor
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

class PyNewTaskCheckerProvider : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = PyCodeExecutor()

  override val envChecker: EnvironmentChecker
    get() = PyEnvironmentChecker()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
    PyNewEduTaskChecker(task, envChecker, project)
}
