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
    if (sdk is PySdkToCreateVirtualEnv) {
      val homePath = sdk.homePath ?: error("Home path is not passed during fake python sdk creation")
      createAndAddVirtualEnv(project, projectSettings, PyDetectedSdk(homePath))
      sdk = projectSettings.sdk
    }
    sdk = updateSdkIfNeeded(project, sdk)
    SdkConfigurationUtil.setDirectoryProjectSdk(project, sdk)
    if (sdk == null) {
      return
    }
    installRequiredPackages(project, sdk)
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }

  private fun createAndAddVirtualEnv(project: Project, settings: PyProjectSettings, baseSdk: PyDetectedSdk) {
    val virtualEnvPath = project.basePath + "/.idea/VirtualEnvironment"
    val existingSdks = PyConfigurableInterpreterList.getInstance(null).allPythonSdks
    val module = ModuleManager.getInstance(project).sortedModules.firstOrNull()
    val sdk = try {
      createVirtualEnvAndSdkSynchronously(
        baseSdk = baseSdk,
        existingSdks = existingSdks,
        venvRoot = virtualEnvPath,
        projectBasePath = project.basePath,
        project = project,
        module = module
      )
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
      if (sdk !is PyDetectedSdk) {
        return sdk
      }
      val name = sdk.name
      val sdkHome = WriteAction.compute<VirtualFile, RuntimeException> { LocalFileSystem.getInstance().refreshAndFindFileByPath(name) }
      val newSdk = SdkConfigurationUtil.createAndAddSDK(sdkHome.path, PythonSdkType.getInstance())
      if (newSdk != null) {
        @Suppress("UnstableApiUsage")
        PythonSdkUpdater.updateOrShowError(newSdk, project, null)
      }
      return newSdk
    }
  }
}
