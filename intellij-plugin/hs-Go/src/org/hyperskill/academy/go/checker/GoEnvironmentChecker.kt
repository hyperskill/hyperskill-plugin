package org.hyperskill.academy.go.checker

import com.goide.sdk.GoSdk
import com.goide.sdk.GoSdkService
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import org.hyperskill.academy.go.messages.EduGoBundle
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_GO
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task

class GoEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val taskDir = task.getDir(project.courseDir) ?: return null
    val module = ModuleUtil.findModuleForFile(taskDir, project) ?: return null
    if (GoSdkService.getInstance(project).getSdk(module) == GoSdk.NULL) {
      return CheckResult(CheckStatus.Unchecked, EduGoBundle.message("error.no.sdk", ENVIRONMENT_CONFIGURATION_LINK_GO))
    }
    return null
  }
}