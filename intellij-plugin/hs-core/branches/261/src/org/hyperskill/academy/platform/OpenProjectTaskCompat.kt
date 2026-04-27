package org.hyperskill.academy.platform

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

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
      this.isNewProject = isNewProject
      this.isProjectCreatedWithWizard = isProjectCreatedWithWizard
      this.runConfigurators = runConfigurators
      this.projectName = projectName
      this.projectToClose = projectToClose
      this.beforeInit = beforeInit
      this.preparedToOpen = { module ->
        preparedToOpen?.invoke(module.project, module)
      }
    }
  }
}
