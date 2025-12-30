package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.invokeAndWaitIfNeeded
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
    if (sdk is PySdkToInstall) {
      val selectedSdk = sdk

      @Suppress("UnstableApiUsage")
      val installedSdk = invokeAndWaitIfNeeded {
        selectedSdk.install(null) {
          detectSystemWideSdks(null, emptyList())
        }.getOrElse {
          LOG.warn(it)
          null
        }
      }
      if (installedSdk != null) {
        createAndAddVirtualEnv(project, projectSettings, installedSdk)
        sdk = projectSettings.sdk
      }
    }
    else if (sdk is PySdkToCreateVirtualEnv) {
      val homePath = sdk.homePath ?: error("Home path is not passed during fake python sdk creation")
      createAndAddVirtualEnv(project, projectSettings, PyDetectedSdk(homePath))
      sdk = projectSettings.sdk
    }
    sdk = updateSdkIfNeeded(project, sdk)
    LOG.warn("PyCourseProjectGenerator: After updateSdkIfNeeded, sdk = ${sdk?.name} at ${sdk?.homePath}")
    SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
    LOG.warn("PyCourseProjectGenerator: After setDirectoryProjectSdk")
    if (sdk == null) {
      LOG.warn("PyCourseProjectGenerator: SDK is null, skipping package installation")
      return
    }
    LOG.warn("PyCourseProjectGenerator: About to install packages")
    installRequiredPackages(project, sdk)
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }

  private fun createAndAddVirtualEnv(project: Project, settings: PyProjectSettings, baseSdk: PyDetectedSdk) {
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
      return
    }
    settings.sdk = sdk
    SdkConfigurationUtil.addSdk(sdk)

    if (module != null) {
      setAssociationToModule(sdk, module)
    }
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
