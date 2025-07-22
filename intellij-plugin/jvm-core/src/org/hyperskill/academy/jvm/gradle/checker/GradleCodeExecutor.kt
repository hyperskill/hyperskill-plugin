package org.hyperskill.academy.jvm.gradle.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.checker.CodeExecutor
import org.hyperskill.academy.learning.checker.DefaultCodeExecutor
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.Task

open class GradleCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> =
    when {
      // TODO https://youtrack.jetbrains.com/issue/EDU-3272
      input != null -> DefaultCodeExecutor().execute(project, task, indicator, input)
      else -> runGradleRunTask(project, task, indicator)
    }

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? = GradleStderrAnalyzer.tryToGetCheckResult(errorOutput)
}