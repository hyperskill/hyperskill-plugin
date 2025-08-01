package org.hyperskill.academy.learning.configuration

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.checker.CodeExecutor
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.PlainTextTaskCheckerProvider.Companion.CHECK_RESULT_FILE
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * To specify check result for plain text courses in tests:
 * 1. Add file called [CHECK_RESULT_FILE] to a task.
 * 2. [CHECK_RESULT_FILE] is used to determine check result.
 * 3. Check result is specified in the following format: `status message`
 *
 * **Examples**:
 *
 * `Solved Great job!`
 *
 * `Failed Incorrect`
 *
 * `Unchecked Failed to check`*
 */
@Suppress("HardCodedStringLiteral")
class PlainTextTaskCheckerProvider : TaskCheckerProvider {
  override val codeExecutor: CodeExecutor
    get() {
      return object : CodeExecutor {
        override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
          val taskDir = task.getDir(project.courseDir) ?: return Err(CheckResult(CheckStatus.Unchecked, "No taskDir in tests"))
          val checkResultFile = taskDir.findChild(CHECK_RESULT_FILE)
                                ?: return Err(CheckResult(CheckStatus.Unchecked, "No $CHECK_RESULT_FILE file"))
          return Ok(VfsUtil.loadText(checkResultFile))
        }
      }
    }

  override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
    return object : TaskChecker<EduTask>(task, project) {
      override fun check(indicator: ProgressIndicator): CheckResult {
        if (task is RemoteEduTask) {
          error("No check for remote tasks")
        }
        val taskDir = task.getDir(project.courseDir) ?: error("No taskDir in tests")
        val checkResultFile = taskDir.findChild(CHECK_RESULT_FILE)

        val testFiles = task.taskFiles.values.filter { false }
        for (testFile in testFiles) {
          val vTestFile = taskDir.findFileByRelativePath(testFile.name) ?: error("No virtual test file found")
          invokeAndWaitIfNeeded {
            if (testFile.contents.textualRepresentation != vTestFile.document.text) {
              error("Saved text for the test file doesn't match actual text")
            }
          }
        }

        if (checkResultFile == null) {
          return CheckResult.SOLVED
        }
        return checkResultFile.checkResult
      }
    }
  }

  private val VirtualFile.checkResult: CheckResult
    get() = try {
      val text = VfsUtil.loadText(this)
      val statusWithMessage = text.split(" ", limit = 2)
      val message = if (statusWithMessage.size > 1) statusWithMessage[1] else ""
      CheckResult(CheckStatus.valueOf(statusWithMessage[0]), message)
    }
    catch (e: IOException) {
      CheckResult.SOLVED
    }
    catch (e: IllegalArgumentException) {
      CheckResult.SOLVED
    }

  companion object {
    @NonNls
    const val CHECK_RESULT_FILE = "checkResult.txt"
  }
}
