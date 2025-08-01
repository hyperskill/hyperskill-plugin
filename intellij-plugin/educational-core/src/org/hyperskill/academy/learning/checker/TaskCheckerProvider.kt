package org.hyperskill.academy.learning.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.tasks.*

/**
 * If you add any new methods here, please do not forget to add it also to
 * @see org.hyperskill.academy.learning.stepik.hyperskill.checker.HyperskillTaskCheckerProvider
 */
interface TaskCheckerProvider {
  val codeExecutor: CodeExecutor
    get() = DefaultCodeExecutor()

  val envChecker: EnvironmentChecker
    get() = EnvironmentChecker()

  fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask>?

  // Should not be overloaded by anyone
  fun getTaskChecker(task: Task, project: Project): TaskChecker<*>? {
    return when (task) {
      is RemoteEduTask,
      is CodeTask,
      is TheoryTask,
      is UnsupportedTask -> null

      is EduTask -> getEduTaskChecker(task, project)
      is OutputTask -> OutputTaskChecker(task, envChecker, project, codeExecutor)
      is IdeTask -> IdeTaskChecker(task, project)
      else -> throw IllegalStateException("Unknown task type: " + task.itemType)
    }
  }
}