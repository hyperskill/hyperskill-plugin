package org.hyperskill.academy.jvm.gradle.checker

import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.checker.EduTaskCheckerBase
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.checker.tests.TestResultCollector
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask

open class NewGradleEduTaskChecker(task: EduTask, envChecker: EnvironmentChecker, project: Project) :
  EduTaskCheckerBase(task, envChecker, project) {

  override fun computePossibleErrorResult(indicator: ProgressIndicator, stderr: String): CheckResult {
    return GradleCommandLine.create(project, ":${getGradleProjectName(task)}:testClasses")?.launchAndCheck(indicator)
           ?: CheckResult.failedToCheck
  }

  override fun createDefaultTestConfigurations(): List<RunnerAndConfigurationSettings> {
    return withGradleTestRunner(project, task) {
      createTestConfigurationsForTestDirectories()
    }.orEmpty()
  }

  override fun createTestResultCollector(): TestResultCollector = GradleTestResultCollector()
}
