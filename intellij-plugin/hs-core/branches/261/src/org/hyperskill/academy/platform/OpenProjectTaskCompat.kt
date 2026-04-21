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
    return OpenProjectTask(
      forceOpenInNewFrame = forceOpenInNewFrame,
      forceReuseFrame = false,
      projectToClose = projectToClose,
      isNewProject = isNewProject,
      useDefaultProjectAsTemplate = true,
      project = null,
      projectName = projectName,
      showWelcomeScreen = true,
      callback = null,
      line = -1,
      column = -1,
      isRefreshVfsNeeded = false,
      runConfigurators = runConfigurators,
      runConversionBeforeOpen = true,
      projectWorkspaceId = null,
      projectFrameTypeId = null,
      isProjectCreatedWithWizard = isProjectCreatedWithWizard,
      preloadServices = false,
      beforeInit = if (beforeInit != null) { { beforeInit(it) } } else null,
      beforeOpen = null,
      preparedToOpen = if (preparedToOpen != null) { { preparedToOpen(it.project, it) } } else null,
      preventIprLookup = false,
      processorChooser = null,
      implOptions = null,
      projectRootDir = null,
      createModule = true
    )
  }
}
