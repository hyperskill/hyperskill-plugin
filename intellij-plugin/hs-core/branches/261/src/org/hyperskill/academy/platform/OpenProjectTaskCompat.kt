package org.hyperskill.academy.platform

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import java.nio.file.Path

/**
 * Compatibility helper to construct OpenProjectTask for 253+.
 */
object OpenProjectTaskCompat {

  @JvmStatic
  fun buildForOpen(
    forceOpenInNewFrame: Boolean,
    isNewProject: Boolean,
    isProjectCreatedWithWizard: Boolean,
    runConfigurators: Boolean,
    projectName: String?,
    projectToClose: Project?,
    beforeInit: ((Project) -> Unit)? = null,
    preparedToOpen: ((Project, Module) -> Unit)? = null
  ): OpenProjectTask {
    return OpenProjectTask {
      this.forceOpenInNewFrame = forceOpenInNewFrame
      this.forceReuseFrame = false
      this.isNewProject = isNewProject
      this.isProjectCreatedWithWizard = isProjectCreatedWithWizard
      this.runConfigurators = runConfigurators
      this.projectName = projectName
      this.projectToClose = projectToClose
      this.createModule = true
      this.useDefaultProjectAsTemplate = true
      this.runConversionBeforeOpen = true
      this.showWelcomeScreen = true
      this.preloadServices = false
      if (beforeInit != null) this.beforeInit = { beforeInit(it) }
      if (preparedToOpen != null) this.preparedToOpen = { preparedToOpen(it.project, it) }
    }
  }
}
