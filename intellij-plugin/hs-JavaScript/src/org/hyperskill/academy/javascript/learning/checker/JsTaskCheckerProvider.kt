package org.hyperskill.academy.javascript.learning.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

open class JsTaskCheckerProvider : TaskCheckerProvider {
  override val envChecker: EnvironmentChecker
    get() = JsEnvironmentChecker()

  override val codeExecutor: JsCodeExecutor
    get() = JsCodeExecutor()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return JsTaskChecker(task, envChecker, project)
  }
}
