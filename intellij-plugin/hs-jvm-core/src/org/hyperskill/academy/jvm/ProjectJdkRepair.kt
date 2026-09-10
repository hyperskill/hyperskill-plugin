package org.hyperskill.academy.jvm

import com.intellij.openapi.application.edtWriteAction
import com.intellij.openapi.application.readAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import org.hyperskill.academy.learning.StudyTaskManager

private val LOG = logger<ProjectJdkRepair>()

/**
 * Restores the JDK of a course project that was generated earlier.
 *
 * A project stores the *name* of its JDK, not the JDK itself, so uninstalling that JDK leaves the project pointing at
 * nothing. The platform then guesses a replacement from the name alone -- an entry named `24` that actually held Java
 * 23 makes it offer to download JDK 24 -- and the learner ends up on a JDK the course checker rejects, or on none at
 * all. The course knows the version it needs, so the plugin restores that one instead: an already installed JDK when
 * there is a suitable one, a downloaded one otherwise.
 */
object ProjectJdkRepair {

  suspend fun ensureProjectJdk(project: Project) {
    val course = StudyTaskManager.getInstance(project).course ?: return
    val requiredVersion = course.requiredJdkVersion as? JavaVersionParseSuccess ?: return

    val currentJdk = readAction { ProjectRootManager.getInstance(project).projectSdk }
    if (JdkLanguageSettings.isSuitableJdk(currentJdk, requiredVersion)) return

    val jdk = suitableJdk(project, currentJdk, requiredVersion) ?: return
    edtWriteAction {
      // Course generation may have set a JDK while the download above was running: a post-startup activity is not
      // sequenced after it, so on a freshly generated project the two run at the same time.
      val installedJdk = ProjectRootManager.getInstance(project).projectSdk
      if (JdkLanguageSettings.isSuitableJdk(installedJdk, requiredVersion)) {
        LOG.info("Project JDK ${installedJdk?.name} was set while ${jdk.name} was being prepared, keeping it")
        return@edtWriteAction
      }
      LOG.info("Replaced project JDK ${installedJdk?.name} with ${jdk.name} required by ${course.name}")
      ProjectRootManager.getInstance(project).projectSdk = jdk
    }
  }

  private suspend fun suitableJdk(project: Project, currentJdk: Sdk?, requiredVersion: JavaVersionParseSuccess): Sdk? {
    val installedJdk = JdkLanguageSettings.findSuitableJdkFromTable(requiredVersion)
    if (installedJdk != null) return installedJdk

    LOG.info("No installed JDK fits ${requiredVersion.javaSdkVersion.description}, downloading it")
    val downloadedJdk = JdkAutoInstaller.installJdkInBackground(project, requiredVersion.javaSdkVersion)
    if (downloadedJdk == null) {
      // Nothing else to do: the checker will tell the learner to update the SDK by hand
      LOG.warn("Failed to provide JDK ${requiredVersion.javaSdkVersion.description}, keeping ${currentJdk?.name}")
    }
    return downloadedJdk
  }
}
