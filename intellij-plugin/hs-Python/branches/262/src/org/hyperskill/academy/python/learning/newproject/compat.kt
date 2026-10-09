package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.PySdkToInstallCompat
import com.jetbrains.python.sdk.PythonSdkInstallBridge
import com.jetbrains.python.sdk.detectSystemWideSdks
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor

private val LOG = logger<PyCourseProjectGenerator>()

/**
 * Python 262 requires every SDK used to launch Python to have [com.jetbrains.python.sdk.PythonSdkAdditionalData].
 * [PySdkToCreateVirtualEnv] already has it; converting it to a [com.jetbrains.python.sdk.PyDetectedSdk]
 * would discard that data and make virtual environment creation fail.
 */
internal fun prepareSdkForVirtualEnvCreation(sdk: PySdkToCreateVirtualEnv): Sdk = sdk

@Suppress("DEPRECATION_ERROR")
internal fun installSdkIfSuggested(sdk: Sdk?): Sdk? {
  if (sdk !is PySdkToInstallCompat) return null

  return invokeAndWaitIfNeeded {
    try {
      PythonSdkInstallBridge.install(sdk.suggestion, null) {
        detectSystemWideSdks(null, emptyList())
      }
    }
    catch (e: Throwable) {
      LOG.warn(e)
      null
    }
  }
}

/**
 * Language level of the interpreter at [homePath], detected by its [flavor].
 */
internal fun languageLevelOf(flavor: PythonSdkFlavor<*>, homePath: String): LanguageLevel? = flavor.getLanguageLevel(homePath)
