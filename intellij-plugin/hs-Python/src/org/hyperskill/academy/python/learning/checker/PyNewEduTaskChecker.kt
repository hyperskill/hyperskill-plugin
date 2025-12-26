package org.hyperskill.academy.python.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiElement
import com.jetbrains.python.testing.AbstractPythonTestRunConfiguration
import com.jetbrains.python.testing.PythonTestConfigurationType
import com.jetbrains.python.testing.TestRunnerService
import org.hyperskill.academy.learning.checker.EduTaskCheckerBase
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.tests.TestResultCollector
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.messages.EduFormatBundle
import org.hyperskill.academy.python.learning.installRequiredPackages
import org.hyperskill.academy.python.learning.messages.EduPythonBundle

class PyNewEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val module = ModuleManager.getInstance(project).modules.singleOrNull()
    TestRunnerService.getInstance(module).selectedFactory = PythonTestConfigurationType.getInstance().unitTestFactory
    // In general, python plugin can create run configuration for a directory,
    // but it can skip some test files if they haven't proper names
    return createTestConfigurationsForTestFiles()
  }

  override fun createTestConfigurationFromPsiElement(element: PsiElement): RunnerAndConfigurationSettings? {
    val configuration = super.createTestConfigurationFromPsiElement(element)
    configuration?.setTaskDirAsWorking()
    return configuration
  }

  override fun ConfigurationContext.selectPreferredConfiguration(): RunnerAndConfigurationSettings? {
    return configuration?.takeIf { it.configuration is AbstractPythonTestRunConfiguration<*> }
  }

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
    // Check for common Python errors first
    PyStderrAnalyzer.tryToGetCheckResult(stderr)?.let { return it }

    // Check if the error is related to missing dependencies (hs-test-python)
    if (isMissingDependenciesError(stderr)) {
      val sdk = ProjectRootManager.getInstance(project).projectSdk
      if (sdk != null) {
        val message = """${EduFormatBundle.message("check.no.tests")}. ${EduPythonBundle.message("python.dependencies.install.suggestion")}"""
        return CheckResult(
          CheckStatus.Unchecked,
          message,
          hyperlinkAction = { installRequiredPackages(project, sdk) }
        )
      }
    }

    return CheckResult.SOLVED
  }

  private fun isMissingDependenciesError(stderr: String): Boolean {
    // Check for common import errors related to hs-test-python
    return stderr.contains("ModuleNotFoundError") && stderr.contains("hstest") ||
           stderr.contains("ImportError") && stderr.contains("hstest") ||
           stderr.contains("No module named 'hstest'") ||
           stderr.contains("No module named \"hstest\"")
  }

  override fun createTestResultCollector(): TestResultCollector = PyTestResultCollector()

  private fun RunnerAndConfigurationSettings.setTaskDirAsWorking() {
    val pythonConfiguration = configuration as? AbstractPythonTestRunConfiguration<*>
    pythonConfiguration?.workingDirectory = task.getDir(project.courseDir)?.path
  }
}
