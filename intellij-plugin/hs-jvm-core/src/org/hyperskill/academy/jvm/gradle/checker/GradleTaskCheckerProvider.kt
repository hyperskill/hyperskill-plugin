package org.hyperskill.academy.jvm.gradle.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.CodeExecutor
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

open class GradleTaskCheckerProvider : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() = GradleCodeExecutor()

  override val envChecker: EnvironmentChecker
    get() = GradleEnvironmentChecker()

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> =
    NewGradleEduTaskChecker(task, envChecker, project)
}
