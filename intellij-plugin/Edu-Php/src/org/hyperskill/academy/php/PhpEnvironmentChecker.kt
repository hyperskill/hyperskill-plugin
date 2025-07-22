package org.hyperskill.academy.php

import com.intellij.openapi.project.Project
import com.jetbrains.php.config.PhpProjectConfigurationFacade
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.php.messages.EduPhpBundle

class PhpEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val interpreter = PhpProjectConfigurationFacade.getInstance(project).interpreter
    return if (interpreter == null) {
      CheckResult(CheckStatus.Unchecked, EduPhpBundle.message("error.no.php.interpreter", EduNames.ENVIRONMENT_CONFIGURATION_LINK_PHP))
    }
    else null
  }
}