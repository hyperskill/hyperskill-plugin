package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SlowOperations
import com.jetbrains.python.configuration.PyConfigurableInterpreterList
import com.jetbrains.python.sdk.*
import com.jetbrains.python.sdk.configuration.createVirtualEnvAndSdkSynchronously
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.python.learning.installRequiredPackages
import org.hyperskill.academy.python.learning.setAssociationToModule

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

    private fun updateSdkIfNeeded(project: Project, sdk: Sdk?): Sdk? {
      LOG.warn("updateSdkIfNeeded: sdk = ${sdk?.name}, type = ${sdk?.javaClass?.simpleName}, homePath = ${sdk?.homePath}")
      if (sdk !is PyDetectedSdk) {
        LOG.warn("updateSdkIfNeeded: SDK is not PyDetectedSdk, returning as is")
        return sdk
      }
      LOG.warn("updateSdkIfNeeded: SDK is PyDetectedSdk, using sdk.name = ${sdk.name}")
      val name = sdk.name
      val sdkHome = WriteAction.compute<VirtualFile, RuntimeException> {
        LocalFileSystem.getInstance().refreshAndFindFileByPath(name)
      }
      LOG.warn("updateSdkIfNeeded: sdkHome = ${sdkHome?.path}")
      val newSdk = SdkConfigurationUtil.createAndAddSDK(sdkHome.path, PythonSdkType.getInstance())
      if (newSdk != null) {
        LOG.warn("updateSdkIfNeeded: New SDK created: ${newSdk.name}, updating...")
        @Suppress("UnstableApiUsage")
        PythonSdkUpdater.updateOrShowError(newSdk, project, null)
      }
      else {
        LOG.warn("updateSdkIfNeeded: Failed to create new SDK")
      }
      return newSdk
    }
  }
}
