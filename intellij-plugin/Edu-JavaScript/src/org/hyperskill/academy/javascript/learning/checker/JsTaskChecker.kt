package org.hyperskill.academy.javascript.learning.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.project.Project
import org.hyperskill.academy.javascript.learning.installNodeDependencies
import org.hyperskill.academy.javascript.learning.messages.EduJavaScriptBundle
import org.hyperskill.academy.learning.checker.EduTaskCheckerBase
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.tests.TestResultCollector
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.messages.EduFormatBundle

open class JsTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) : EduTaskCheckerBase(task, envChecker, project) {

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return createTestConfigurationsForTestFiles()
  }

  override fun validateConfiguration(configuration: RunnerAndConfigurationSettings): CheckResult? {
    return try {
      configuration.checkSettings()
      null
    }
    catch (e: RuntimeConfigurationError) {
      val packageJson = project.courseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON) ?: return null
      val message = """${EduFormatBundle.message("check.no.tests")}. ${EduJavaScriptBundle.message("install.dependencies")}."""
      CheckResult(CheckStatus.Unchecked, message, hyperlinkAction = { installNodeDependencies(project, packageJson) })
    }
  }

  override fun createTestResultCollector(): TestResultCollector = JsTestResultCollector()
}
