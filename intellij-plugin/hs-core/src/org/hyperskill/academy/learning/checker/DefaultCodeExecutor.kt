package org.hyperskill.academy.learning.checker

import com.intellij.execution.ExecutionListener
import com.intellij.execution.OutputListener
import com.intellij.execution.configurations.RuntimeConfigurationException
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.checker.CheckUtils.EXECUTION_ERROR_MESSAGE
import org.hyperskill.academy.learning.checker.CodeExecutor.Companion.resultUnchecked
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.runReadActionInSmartMode
import org.jetbrains.annotations.NonNls
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

open class DefaultCodeExecutor : CodeExecutor {
  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    fun logAndQuit(error: String): Err<CheckResult> {
      LOG.warn(error)
      return resultUnchecked(error)
    }

    val configuration = runReadActionInSmartMode(project) { createRunConfiguration(project, task) }
    if (configuration == null) {
      LOG.warn("Failed to launch checking. Run configuration is null")
      return Err(CheckResult.failedToCheck)
    }

    try {
      configuration.checkSettings()
    }
    catch (e: RuntimeConfigurationException) {
      LOG.warn("Failed to launch checking", e)
      return Err(CheckResult.failedToCheck)
    }

    configuration.isActivateToolWindowBeforeRun = false

    var processNotStarted = false
    val executionListener = object : ExecutionListener {
      override fun processNotStarted(executorId: String, e: ExecutionEnvironment) {
        processNotStarted = true
      }

      override fun processStarted(executorId: String, env: ExecutionEnvironment, handler: ProcessHandler) {
        if (input != null) {
          try {
            val out = BufferedWriter(OutputStreamWriter(handler.processInput!!, StandardCharsets.UTF_8))
            out.write(input)
            out.write("\n")
            out.flush()
          }
          catch (e: IOException) {
            LOG.warn("Failed to write input", e)
          }
        }
      }
    }

    val processListener = OutputListener()

    if (!CheckUtils.executeRunConfigurations(project, listOf(configuration), indicator, executionListener, processListener))
      return logAndQuit(EduCoreBundle.message("error.execution.failed"))

    if (indicator.isCanceled) return logAndQuit(EduCoreBundle.message("error.execution.canceled"))
    if (processNotStarted) return logAndQuit(EduCoreBundle.message("error.execution.failed"))

    val output = processListener.output
    val errorOutput = output.stderr

    if (output.exitCode != 0) {
      // Sometimes error messages and stack traces are in the stdout instead of stderr. For example, JS
      val outputErrorMessage = errorOutput.ifEmpty { output.stdout }
      val err =
        tryToExtractCheckResultError(outputErrorMessage) ?: CheckResult(CheckStatus.Failed, EXECUTION_ERROR_MESSAGE, outputErrorMessage)
      return Err(err)
    }

    var outputString = output.stdout
    if (outputString.isEmpty()) {
      outputString = NO_OUTPUT
    }
    return Ok(outputString)
  }

  override fun tryToExtractCheckResultError(errorOutput: String): CheckResult? = null

  companion object {
    private val LOG = Logger.getInstance(DefaultCodeExecutor::class.java)

    @NonNls
    const val NO_OUTPUT: String = "<no output>"
  }
}
