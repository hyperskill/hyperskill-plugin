package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SlowOperations
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.sdk.*
import com.jetbrains.python.sdk.configuration.createVirtualEnvAndSdkSynchronously
import com.jetbrains.python.sdk.flavors.PyFlavorAndData
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.python.learning.installRequiredPackages
import org.hyperskill.academy.python.learning.setAssociationToModule
import org.jetbrains.annotations.VisibleForTesting

open class PyCourseProjectGenerator(
  builder: EduCourseBuilder<PyProjectSettings>,
  course: Course
) : CourseProjectGenerator<PyProjectSettings>(builder, course) {

  override fun afterProjectGenerated(
    project: Project,
    projectSettings: PyProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    var sdk = projectSettings.sdk
    // Platform-dependent: installs Python first if the selected SDK is an "install Python" suggestion.
    // See `installSdkIfSuggested` implementations in `branches/<version>/src`.
    val installedSdk = installSdkIfSuggested(sdk)
    if (installedSdk != null) {
      sdk = createAndAddVirtualEnv(project, projectSettings, installedSdk)
    }
    else if (sdk is PySdkToCreateVirtualEnv) {
      sdk = createAndAddVirtualEnv(project, projectSettings, prepareSdkForVirtualEnvCreation(sdk))
    }
    sdk = updateSdkIfNeeded(project, sdk)
    LOG.warn("PyCourseProjectGenerator: After updateSdkIfNeeded, sdk = ${sdk?.name} at ${sdk?.homePath}")
    SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
    LOG.warn("PyCourseProjectGenerator: After setDirectoryProjectSdk")
    if (sdk == null) {
      LOG.warn("PyCourseProjectGenerator: SDK is null, skipping package installation")
    }
    else {
      LOG.warn("PyCourseProjectGenerator: About to install packages")
      installRequiredPackages(project, sdk)
    }
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }

  private fun createAndAddVirtualEnv(project: Project, settings: PyProjectSettings, baseSdk: Sdk): Sdk? {
    val virtualEnvPath = project.basePath + "/.idea/VirtualEnvironment"
    val existingSdks = PyConfigurableInterpreterList.getInstance(null).allPythonSdks
    val module = ModuleManager.getInstance(project).sortedModules.firstOrNull()
    val sdk = try {
      // BACKCOMPAT: 252 - Python SDK's createVirtualEnvAndSdkSynchronously performs slow VFS operations
      // that trigger "Slow operations are prohibited on EDT" when called from modal progress context.
      // Using knownIssue to suppress the warning until Python plugin fixes this.
      SlowOperations.knownIssue("PY-78757").use {
        createVirtualEnvAndSdkSynchronously(
          baseSdk = baseSdk,
          existingSdks = existingSdks,
          venvRoot = virtualEnvPath,
          projectBasePath = project.basePath,
          project = project,
          module = module
        )
      }
    }
    catch (e: Exception) {
      LOG.warn("Failed to create virtual env in $virtualEnvPath", e)
      return null
    }
    settings.sdk = sdk
    SdkConfigurationUtil.addSdk(sdk)

    if (module != null) {
      setAssociationToModule(sdk, module)
    }
    return sdk
  }

  companion object {
    private val LOG = logger<PyCourseProjectGenerator>()

    /**
     * Turns an interpreter picked with the browse button of the interpreter combobox into a registered SDK.
     * Such an interpreter is a [PyDetectedSdk]: a bare path whose name is the path itself.
     *
     * The SDK gets [PythonSdkAdditionalData]: since 262 the Python plugin refuses to launch an interpreter whose SDK has none
     * ("... doesn't have an additional data: it was created by buggy code"), so `SdkConfigurationUtil.createAndAddSDK`
     * failed while setting up the SDK and the project was left without an interpreter.
     */
    @VisibleForTesting
    internal fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
      LOG.warn("updateSdkIfNeeded: sdk = ${sdk?.name}, type = ${sdk?.javaClass?.simpleName}, homePath = ${sdk?.homePath}")
      if (sdk !is PyDetectedSdk) {
        LOG.warn("updateSdkIfNeeded: SDK is not PyDetectedSdk, returning as is")
        return sdk
      }
      val homePath = sdk.homePath?.takeIf { it.isNotBlank() } ?: sdk.name

      val registeredSdk = ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance()).find {
        val registeredHome = it.homePath
        registeredHome != null && FileUtil.pathsEqual(registeredHome, homePath) && it.sdkAdditionalData is PythonSdkAdditionalData
      }
      if (registeredSdk != null) {
        LOG.warn("updateSdkIfNeeded: reusing registered SDK ${registeredSdk.name} for $homePath")
        return registeredSdk
      }

      val sdkHome = WriteAction.compute<VirtualFile?, RuntimeException> {
        LocalFileSystem.getInstance().refreshAndFindFileByPath(FileUtil.toSystemIndependentName(homePath))
      }
      if (sdkHome == null) {
        LOG.warn("updateSdkIfNeeded: interpreter $homePath is not found")
        return null
      }
      val additionalData = createAdditionalData(homePath) ?: return null
      val newSdk = SdkConfigurationUtil.setupSdk(
        ProjectJdkTable.getInstance().allJdks,
        sdkHome,
        PythonSdkType.getInstance(),
        additionalData,
        null
      )
      if (newSdk == null) {
        LOG.warn("updateSdkIfNeeded: failed to set up SDK for $homePath")
        return null
      }
      SdkConfigurationUtil.addSdk(newSdk)
      LOG.warn("updateSdkIfNeeded: created SDK ${newSdk.name} for $homePath, updating...")
      @Suppress("UnstableApiUsage")
      PythonSdkUpdater.updateOrShowError(newSdk, project, null)
      return newSdk
    }

    /**
     * Additional data for an SDK of the local interpreter at [homePath], with the flavor the interpreter is detected as.
     * Flavors that need extra data (like conda environments) cannot be set up from a path alone,
     * so such an interpreter is treated as a plain local one.
     */
    private fun createAdditionalData(homePath: String): PythonSdkAdditionalData? {
      val flavor = (listOfNotNull(PythonSdkFlavor.getFlavor(homePath)) + PythonSdkFlavor.getApplicableFlavors(false))
        .firstOrNull { it.flavorDataClass == PyFlavorData.Empty::class.java }
      if (flavor == null) {
        LOG.warn("updateSdkIfNeeded: no suitable flavor for $homePath")
        return null
      }
      @Suppress("UNCHECKED_CAST")
      return PythonSdkAdditionalData(PyFlavorAndData(PyFlavorData.Empty, flavor as PythonSdkFlavor<PyFlavorData.Empty>))
    }
  }
}
