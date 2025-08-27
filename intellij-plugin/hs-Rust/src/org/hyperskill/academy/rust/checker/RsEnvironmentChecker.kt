package org.hyperskill.academy.rust.checker

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_RUST
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.rust.messages.EduRustBundle
import org.rust.cargo.project.model.cargoProjects
import org.rust.cargo.project.settings.rustSettings

class RsEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val toolchain = project.rustSettings.toolchain
    if (toolchain == null || !toolchain.looksLikeValidToolchain()) {
      return CheckResult(CheckStatus.Unchecked, EduRustBundle.message("error.no.toolchain.location", ENVIRONMENT_CONFIGURATION_LINK_RUST))
    }

    if (!project.cargoProjects.hasAtLeastOneValidProject) {
      return CheckResult(CheckStatus.Unchecked, EduRustBundle.message("error.no.cargo.project", ENVIRONMENT_CONFIGURATION_LINK_RUST))
    }

    return null
  }
}