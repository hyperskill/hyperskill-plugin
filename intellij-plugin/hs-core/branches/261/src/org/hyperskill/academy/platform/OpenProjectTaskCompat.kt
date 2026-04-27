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

    val task = OpenProjectTask(
      forceOpenInNewFrame = forceOpenInNewFrame,
      projectToClose = projectToClose,
      isNewProject = isNewProject
    )
    return task.copy(
      projectName = projectName,
      runConfigurators = runConfigurators,
      isProjectCreatedWithWizard = isProjectCreatedWithWizard,
      beforeInit = beforeInit?.let { { beforeInit(it) } },
      preparedToOpen = preparedToOpen?.let { { preparedToOpen(it.project, it) } },
      forceReuseFrame = false,
      createModule = true,
      useDefaultProjectAsTemplate = true,
      runConversionBeforeOpen = true,
      showWelcomeScreen = true,
      preloadServices = false
    )
  }
}
