package org.hyperskill.academy.rust.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.CodeExecutor
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

class RsTaskCheckerProvider : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = RsCodeExecutor()

  override val envChecker: EnvironmentChecker
    get() = RsEnvironmentChecker()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> = RsEduTaskChecker(project, envChecker, task)
}
