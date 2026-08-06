package org.hyperskill.academy.platform

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project

/**
 * Compatibility helper to construct [OpenProjectTask].
 *
 * The task itself is assembled by [OpenProjectTaskFactory], which is written in Java on purpose - see the explanation
 * there. This object only adapts the callback shapes and must not touch [OpenProjectTask] members directly.
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
    val preparedToOpenSuspend: (suspend (Module) -> Unit)? =
      if (preparedToOpen != null) { { module -> preparedToOpen(module.project, module) } } else null

    return OpenProjectTaskFactory.buildForOpen(
      forceOpenInNewFrame,
      isNewProject,
      isProjectCreatedWithWizard,
      runConfigurators,
      projectName,
      projectToClose,
      beforeInit,
      preparedToOpenSuspend
    )
  }
}
