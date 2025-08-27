package org.hyperskill.academy.jvm.gradle.checker

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdkVersionUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.roots.ui.configuration.ProjectSettingsService
import org.hyperskill.academy.jvm.gradle.GradleCourseRefresher
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils
import org.hyperskill.academy.jvm.hyperskillJdkVersion
import org.hyperskill.academy.jvm.messages.EduJVMBundle
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_GRADLE
import org.hyperskill.academy.learning.RefreshCause
import org.hyperskill.academy.learning.checker.EnvironmentChecker
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.jetbrains.plugins.gradle.settings.GradleSettings

open class GradleEnvironmentChecker : EnvironmentChecker() {
  override fun getEnvironmentError(project: Project, task: Task): CheckResult? {
    val sdk = ProjectRootManager.getInstance(project).projectSdk ?: return noSdkConfiguredResult

    if (task.course is HyperskillCourse && JavaSdkVersionUtil.getJavaSdkVersion(sdk) != hyperskillJdkVersion) {
      return getIncorrectHyperskillJDKResult(project)
    }

    val taskDir = task.getDir(project.courseDir) ?: return getFailedToLaunchCheckingResult(project)
    val module = ModuleUtil.findModuleForFile(taskDir, project) ?: return getFailedToLaunchCheckingResult(project)

    val path = ExternalSystemApiUtil.getExternalRootProjectPath(module) ?: return getGradleNotImportedResult(project)
    GradleSettings.getInstance(project).getLinkedProjectSettings(path) ?: return getGradleNotImportedResult(project)
    return null
  }

  companion object {
    private fun reloadGradle(project: Project) {
      EduGradleUtils.updateGradleSettings(project)
      EduGradleUtils.setupGradleProject(project)
      val refresher = GradleCourseRefresher.firstAvailable() ?: error("Can not find Gradle course refresher")
      refresher.refresh(project, RefreshCause.STRUCTURE_MODIFIED)
    }

    private const val RELOAD_GRADLE_LINK: String = "reload_gradle"

    private val noSdkConfiguredResult: CheckResult
      get() = CheckResult(CheckStatus.Unchecked, EduCoreBundle.message("error.no.sdk.gradle", ENVIRONMENT_CONFIGURATION_LINK_GRADLE))

    fun getFailedToLaunchCheckingResult(project: Project): CheckResult {
      return CheckResult(
        CheckStatus.Unchecked,
        EduCoreBundle.message(
          "error.failed.to.launch.checking.with.reload.gradle.message", RELOAD_GRADLE_LINK,
          EduFormatNames.NO_TESTS_URL
        ), hyperlinkAction = { reloadGradle(project) })
    }

    private fun getGradleNotImportedResult(project: Project): CheckResult {
      return CheckResult(
        CheckStatus.Unchecked,
        EduJVMBundle.message("error.gradle.not.imported", RELOAD_GRADLE_LINK, EduFormatNames.NO_TESTS_URL),
        hyperlinkAction = { reloadGradle(project) })
    }

    private const val OPEN_MODULE_SETTINGS = "open_module_settings"

    private fun getIncorrectHyperskillJDKResult(project: Project): CheckResult {
      return CheckResult(
        CheckStatus.Unchecked,
        EduJVMBundle.message("error.hyperskill.incorrect.jdk", hyperskillJdkVersion.description, OPEN_MODULE_SETTINGS),
        hyperlinkAction = { openModuleSettings(project) }
      )
    }

    private fun openModuleSettings(project: Project) {
      ProjectSettingsService.getInstance(project).openProjectSettings()
    }
  }
}
