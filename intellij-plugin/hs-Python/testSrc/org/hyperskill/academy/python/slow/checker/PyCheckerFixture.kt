package org.hyperskill.academy.python.slow.checker

import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.impl.SdkConfigurationUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.runInEdtAndWait
import com.jetbrains.python.sdk.PythonSdkType
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import org.hyperskill.academy.learning.checker.EduCheckerFixture
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings
import org.hyperskill.academy.python.learning.newproject.PySdkToCreateVirtualEnv
import java.nio.file.Paths

class PyCheckerFixture : EduCheckerFixture<PyProjectSettings>() {
  override val projectSettings: PyProjectSettings = PyProjectSettings()

  private val sdkLocation: String? by lazy {
    val location = System.getenv(PYTHON_SDK) ?: return@lazy null
    Paths.get(location).toRealPath().toString()
  }

  override fun setUp() {
    val sdkLocation = sdkLocation ?: return
    runInEdtAndWait {
      val versionString = PythonSdkFlavor.getApplicableFlavors(false)[0].getVersionString(sdkLocation)
                          ?: error("Can't get python version")
      projectSettings.sdk = PySdkToCreateVirtualEnv.create(versionString, sdkLocation, versionString)
      VfsRootAccess.allowRootAccess(testRootDisposable, sdkLocation)
    }
  }

  override fun tearDown() {
    for (pythonSdk in ProjectJdkTable.getInstance().getSdksOfType(PythonSdkType.getInstance())) {
      SdkConfigurationUtil.removeSdk(pythonSdk)
    }
  }

  override fun getSkipTestReason(): String? {
    return if (sdkLocation == null) {
      "No Python SDK location defined. Use `$PYTHON_SDK` environment variable to provide sdk location"
    }
    else {
      super.getSkipTestReason()
    }
  }

  companion object {
    private const val PYTHON_SDK = "PYTHON_SDK"
  }
}
