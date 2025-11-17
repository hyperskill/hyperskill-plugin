package org.hyperskill.academy.ai.debugger.core.utils

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.readText
import com.jetbrains.educational.ml.debugger.dto.ProgrammingLanguage
import com.jetbrains.educational.ml.debugger.dto.TaskDescriptionFormat
import org.hyperskill.academy.ai.debugger.core.api.TestFinder
import org.hyperskill.academy.ai.debugger.core.service.TestInfo
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.DescriptionFormat
import org.hyperskill.academy.learning.courseFormat.EduTestInfo.Companion.firstFailed
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.getDescriptionFile
import org.hyperskill.academy.learning.courseFormat.ext.getText
import org.hyperskill.academy.learning.courseFormat.ext.languageById
import org.hyperskill.academy.learning.courseFormat.tasks.Task

object AIDebugUtils {

  fun CheckResult.failedTestName() = executedTestsInfo.firstFailed()?.name?.substringBefore("(")
                                     ?: error("No failed test is found")

  fun Project.language() = course?.languageById ?: error("Language is not found")

  fun <T> runWithTests(execution: () -> T, executionStopped: () -> Unit = {}): T? =
    runCatching {
      execution()
    }.onFailure {
      LOG.error("Failed to start execution")
      executionStopped()
    }.getOrNull()

  private fun Task.getAllTestFiles() = taskFiles.values.filter {
    EduUtilsKt.isTestsFile(this, it.name)
  }

  private val LOG: Logger = Logger.getInstance(AIDebugUtils::class.java)

  fun List<TaskFile>.toNameTextMap(project: Project) = runReadAction { associate { it.name to (it.getText(project) ?: "") } }

  fun DescriptionFormat.toTaskDescriptionType() =
    when (this) {
      DescriptionFormat.MD -> TaskDescriptionFormat.MD
      DescriptionFormat.HTML -> TaskDescriptionFormat.HTML
    }

  fun Task.getTaskDescriptionText(project: Project) = runReadAction {
    getDescriptionFile(project, guessFormat = true)?.readText()
  } ?: error("There are no description for the task")

  fun Project.getLanguage(): ProgrammingLanguage =
    when (course?.languageId) {
      "kotlin" -> ProgrammingLanguage.KOTLIN
      "JAVA" -> ProgrammingLanguage.JAVA
      else -> error("Language is not supported")
    }

  fun CheckResult.collectTestInfo(project: Project, task: Task): TestInfo {
    val testName = failedTestName()
    val testText = TestFinder.findTestByName(project, task, testName)
    val testFiles = runWithTests({
      task.getAllTestFiles().toNameTextMap(project)
    }) ?: emptyMap()
    return TestInfo(
      errorMessage = details ?: message,
      expectedOutput = diff?.expected ?: "There are no expected output",
      name = testName,
      testFiles = testFiles,
      text = testText
    )
  }

}
