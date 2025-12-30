package org.hyperskill.academy.python.learning.checker

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.roots.ProjectRootManager
import com.jetbrains.python.sdk.pythonSdk
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_PYTHON
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.python.learning.messages.EduPythonBundle

class PyEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val projectSdk = project.pythonSdk
    val projectRootSdk = ProjectRootManager.getInstance(project).projectSdk
    val allSdks = ProjectJdkTable.getInstance().allJdks
    val modules = ModuleManager.getInstance(project).modules

    LOG.warn("PyEnvironmentChecker: project.pythonSdk = ${projectSdk?.name} at ${projectSdk?.homePath}")
    LOG.warn("PyEnvironmentChecker: ProjectRootManager.projectSdk = ${projectRootSdk?.name} at ${projectRootSdk?.homePath}")
    LOG.warn("PyEnvironmentChecker: modules count = ${modules.size}")
    modules.forEach { module ->
      LOG.warn("PyEnvironmentChecker: module ${module.name} pythonSdk = ${module.pythonSdk?.name} at ${module.pythonSdk?.homePath}")
    }
    LOG.warn("PyEnvironmentChecker: All registered SDKs:")
    allSdks.forEach { sdk ->
      LOG.warn("  - ${sdk.name} at ${sdk.homePath}")
    }

    return if (projectSdk == null) {
      CheckResult(CheckStatus.Unchecked, EduPythonBundle.message("error.no.python.interpreter", ENVIRONMENT_CONFIGURATION_LINK_PYTHON))
    }
    else null
  }

  companion object {
    private val LOG = logger<PyEnvironmentChecker>()
  }
}