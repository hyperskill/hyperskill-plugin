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
    return OpenProjectTask(
      forceOpenInNewFrame = forceOpenInNewFrame,
      forceReuseFrame = false,
      projectToClose = projectToClose,
      isNewProject = isNewProject,
      useDefaultProjectAsTemplate = false,
      project = null,
      projectName = projectName,
      showWelcomeScreen = true,
      callback = null,
      line = -1,
      column = -1,
      isRefreshVfsNeeded = false,
      runConfigurators = runConfigurators,
      runConversionBeforeOpen = false,
      projectWorkspaceId = null,
      projectFrameTypeId = null,
      isProjectCreatedWithWizard = isProjectCreatedWithWizard,
      preloadServices = false,
      beforeInit = beforeInit,
      beforeOpen = null,
      preparedToOpen = if (preparedToOpen == null) null else { module -> preparedToOpen.invoke(module.project, module) },
      preventIprLookup = false,
      processorChooser = null,
      implOptions = null,
      projectRootDir = null,
      createModule = true
    )
  }
}
