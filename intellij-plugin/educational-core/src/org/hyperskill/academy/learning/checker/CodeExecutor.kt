package org.hyperskill.academy.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.Nls

/**
 * Interface for code execution classes
 */

interface CodeExecutor {
  /**
   * @param input String to pass to stdin
   * @return possible values
   * - [Ok][org.hyperskill.academy.learning.Ok] - with executed code output
   * - [Err][org.hyperskill.academy.learning.Err] - with error message, which usually proceeds to [CheckStatus.Unchecked][org.hyperskill.academy.learning.courseFormat.CheckStatus.Unchecked]
   */
  fun execute(
    project: Project,
    task: Task,
    indicator: ProgressIndicator,
    input: String? = null
  ): Result<String, CheckResult>

  fun createRunConfiguration(
    project: Project,
    task: Task
  ): RunnerAndConfigurationSettings? = CheckUtils.createDefaultRunConfiguration(project, task)

  fun tryToExtractCheckResultError(errorOutput: String): CheckResult? = null

  companion object {
    fun resultUnchecked(
      msg: @Nls(capitalization = Nls.Capitalization.Sentence) String
    ): Err<CheckResult> = Err(CheckResult(CheckStatus.Unchecked, msg))
  }
}