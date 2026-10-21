package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.UserDataHolderBase
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PySdkToInstallCompat
import com.jetbrains.python.sdk.PythonSdkInstallBridge
import com.jetbrains.python.sdk.findBaseSdks
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

private val LOG = logger<PyCourseProjectGenerator>()

/**
 * Python 262 requires every SDK used to launch Python to have [com.jetbrains.python.sdk.PythonSdkAdditionalData].
 * [PySdkToCreateVirtualEnv] already has it; converting it to a [com.jetbrains.python.sdk.PyDetectedSdk]
 * would discard that data and make virtual environment creation fail.
 */
internal fun prepareSdkForVirtualEnvCreation(sdk: PySdkToCreateVirtualEnv): Sdk = sdk

internal fun installSdkIfSuggested(sdk: Sdk?): Sdk? {
  if (sdk !is PySdkToInstallCompat) return null

  return invokeAndWaitIfNeeded {
    try {
      PythonSdkInstallBridge.install(sdk.suggestion, null) {
        // `detectSystemWideSdks` used before 263 is removed from the Python plugin
        findBaseSdks(emptyList(), null, UserDataHolderBase())
      }
    }
    catch (e: Throwable) {
      LOG.warn(e)
      null
    }
  }
}

/**
 * Language level of the interpreter at [homePath].
 *
 * In 263 `PythonSdkFlavor.getLanguageLevel(String)` was removed together with the per-flavor `getVersionString(String)`,
 * so the version is detected the way their base implementation did it, regardless of [flavor].
 */
@Suppress("UNUSED_PARAMETER")
internal fun languageLevelOf(flavor: PythonSdkFlavor<*>, homePath: String): LanguageLevel? =
  PythonSdkFlavor.getLanguageLevelFromVersionStringStatic(PythonSdkFlavor.getVersionStringStatic(homePath))
