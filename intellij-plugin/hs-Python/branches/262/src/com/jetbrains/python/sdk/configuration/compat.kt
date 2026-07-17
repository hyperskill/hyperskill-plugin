package com.jetbrains.python.sdk.configuration

import com.intellij.execution.ExecutionException
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.platform.ide.progress.ModalTaskOwner
import com.intellij.platform.ide.progress.runWithModalProgressBlocking
import com.intellij.python.venv.sdk.flavors.VirtualEnvSdkFlavor
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.python.packaging.PyPackageManagers
import com.jetbrains.python.packaging.PyTargetEnvCreationManager
import com.jetbrains.python.sdk.PythonSdkAdditionalData
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.baseDir
import com.jetbrains.python.sdk.excludeInnerVirtualEnv
import com.jetbrains.python.sdk.flavors.PyFlavorAndData
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.impl.PySdkBundle
import com.jetbrains.python.sdk.setAssociationToModule
import com.jetbrains.python.sdk.setAssociationToPath
import com.jetbrains.python.sdk.targetEnvConfiguration
import com.jetbrains.python.target.ui.TargetPanelExtension
import org.jetbrains.annotations.ApiStatus

/**
 * Reimplementation of `com.jetbrains.python.sdk.configuration.createVirtualEnvAndSdkSynchronously`:
 * in 262 its building blocks (`installSdkIfNeeded`, `createSdkByGenerateTask`, `pyModalBlocking`,
 * `PyTargetAwareAdditionalData.getInterpreterVersion`, `setAssociationToModuleAsync`) became internal
 * or were removed from the Python plugin.
 *
 * TODO: 262 — only local base SDKs are supported, the Targets API branch was dropped.
 */
@ApiStatus.Internal
@RequiresEdt
@Suppress("UNUSED_PARAMETER")
fun createVirtualEnvAndSdkSynchronously(
  baseSdk: Sdk,
  existingSdks: List<Sdk>,
  venvRoot: String,
  projectBasePath: String?,
  project: Project?,
  module: Module?,
  context: UserDataHolder = UserDataHolderBase(),
  inheritSitePackages: Boolean = false,
  makeShared: Boolean = false,
  targetPanelExtension: TargetPanelExtension? = null,
): Sdk {
  if (baseSdk.targetEnvConfiguration != null) {
    // TODO: 262 — target-based SDK creation APIs (`createSdkForTarget` and friends) are internal now
    throw ExecutionException("Creating virtual environments for target-based SDKs is not supported")
  }

  // Historically `installSdkIfNeeded` installed Python first when `baseSdk` was a `PySdkToInstall`.
  // On 262 `PySdkToInstall` is internal and never reaches this method, so `baseSdk` is always an installed SDK.
  val installedSdk: Sdk = baseSdk

  val projectPath = projectBasePath ?: module?.baseDir?.path ?: project?.basePath
  val task = object : Task.WithResult<String, ExecutionException>(project, PySdkBundle.message("python.creating.venv.title"), false) {
    override fun compute(indicator: ProgressIndicator): String {
      indicator.isIndeterminate = true
      val sdk = if (installedSdk is Disposable && Disposer.isDisposed(installedSdk)) {
        ProjectJdkTable.getInstance().findJdk(installedSdk.name)!!
      }
      else {
        installedSdk
      }

      try {
        return PyTargetEnvCreationManager(sdk).createVirtualEnv(venvRoot, inheritSitePackages)
      }
      finally {
        PyPackageManagers.getInstance().clearCache(sdk)
      }
    }
  }
  val venvSdk = createSdkByGenerateTask(task, existingSdks)

  if (!makeShared) {
    when {
      module != null -> runWithModalProgressBlocking(ModalTaskOwner.guess(), "...") { venvSdk.setAssociationToModule(module) }
      projectPath != null -> runWithModalProgressBlocking(ModalTaskOwner.guess(), "...") { venvSdk.setAssociationToPath(projectPath) }
    }
  }

  project?.excludeInnerVirtualEnv(venvSdk)

  // Note: previous versions of this compat code constructed (but never ran) a background task
  // storing the preferred virtual env base path in `PySdkSettings`, so it is intentionally omitted here.

  return venvSdk
}

/**
 * Replacement for `com.jetbrains.python.sdk.createSdkByGenerateTask` removed in 262:
 * runs [generateSdkHomePath] under a progress indicator and sets up an SDK for the resulting interpreter path.
 */
private fun createSdkByGenerateTask(
  generateSdkHomePath: Task.WithResult<String, ExecutionException>,
  existingSdks: List<Sdk>,
): Sdk {
  val homePath = ProgressManager.getInstance().run(generateSdkHomePath)
  val homeFile = StandardFileSystems.local().refreshAndFindFileByPath(homePath)
                 ?: throw ExecutionException("Python interpreter is not found at $homePath")
  val additionalData = PythonSdkAdditionalData(PyFlavorAndData(PyFlavorData.Empty, VirtualEnvSdkFlavor.getInstance()))
  return SdkConfigurationUtil.setupSdk(existingSdks.toTypedArray(), homeFile, PythonSdkType.getInstance(), additionalData, null)
}
