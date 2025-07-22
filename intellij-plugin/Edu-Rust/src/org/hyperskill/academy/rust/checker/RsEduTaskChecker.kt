package org.hyperskill.academy.rust.checker

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.EduTaskCheckerBase
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.tests.TestResultCollector
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.rust.messages.EduRustBundle.message
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings
import org.rust.cargo.project.workspace.CargoWorkspace
import org.rust.cargo.runconfig.command.CargoCommandConfiguration
import org.rust.cargo.runconfig.command.cargoCommandConfigurationType
import org.rust.cargo.runconfig.mergeWithDefault
import org.rust.cargo.toolchain.CargoCommandLine
import org.rust.cargo.toolchain.tools.cargo

class RsEduTaskChecker(project: Project, envChecker: EnvironmentChecker, task: EduTask) : EduTaskCheckerBase(task, envChecker, project) {

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
    val pkg = findCargoPackage() ?: return CheckResult(CheckStatus.Failed, message("error.no.package.for.task", task.name))
    // `--color never` is needed to avoid unexpected color escape codes in output
    val cmd = CargoCommandLine.forPackage(pkg, "test", listOf("--no-run", "--color", "never")).copy(emulateTerminal = false)

    val cargo = project.rustSettings.toolchain?.cargo() ?: return CheckResult(CheckStatus.Failed, message("error.no.toolchain"))
    val processOutput = cargo.toGeneralCommandLine(project, cmd).executeCargoCommandLine()
    return RsStderrAnalyzer.tryToGetCheckResult(processOutput.stdout) ?: super.computePossibleErrorResult(indicator, stderr)
  }

  override val preferredConfigurationType: ConfigurationType
    get() = cargoCommandConfigurationType()

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    val configurations = createTestConfigurationsForTestDirectories().filter { it.configuration.type == preferredConfigurationType }

    return configurations.ifEmpty {
      val pkg = findCargoPackage() ?: return@ifEmpty emptyList()
      val cmd = CargoCommandLine.forPackage(pkg, "test", listOf("--color", "never")).copy(emulateTerminal = false)

      val configurationSetting = RunManager.getInstance(project)
        .createConfiguration("tests", cargoCommandConfigurationType().factory)
      val configuration = configurationSetting.configuration as CargoCommandConfiguration
      cmd.mergeWithDefault(configuration)
      configuration.setFromCmd(cmd)

      listOf(configurationSetting)
    }
  }

  private fun findCargoPackage(): CargoWorkspace.Package? {
    val taskDir = task.getDir(project.courseDir) ?: error("Failed to find directory of `${task.name}` task")
    return runReadAction { project.cargoProjects.findPackageForFile(taskDir) }
  }

  // BACKCOMPAT: 2025.2. Do not override this method
  override fun createTestResultCollector(): TestResultCollector = RsTestResultCollector()
}
