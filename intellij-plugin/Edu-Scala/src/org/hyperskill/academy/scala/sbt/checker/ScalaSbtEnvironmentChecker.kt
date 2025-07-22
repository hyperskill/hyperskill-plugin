package org.hyperskill.academy.scala.sbt.checker

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_SCALA
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.scala.messages.EduScalaBundle
import org.jetbrains.sbt.SbtUtil

class ScalaSbtEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    if (ProjectRootManager.getInstance(project).projectSdk == null) {
      return CheckResult(CheckStatus.Unchecked, EduScalaBundle.message("error.no.sdk", ENVIRONMENT_CONFIGURATION_LINK_SCALA))
    }
    if (!SbtUtil.isSbtProject(project)) {
      return CheckResult(CheckStatus.Unchecked, EduScalaBundle.message("error.no.sbt.project", ENVIRONMENT_CONFIGURATION_LINK_SCALA))
    }
    return null
  }
}
