package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.sdk.PyDetectedSdk
import com.jetbrains.python.sdk.PySdkToInstall
import com.jetbrains.python.sdk.detectSystemWideSdks

private val LOG = logger<PyCourseProjectGenerator>()

internal fun prepareSdkForVirtualEnvCreation(sdk: PySdkToCreateVirtualEnv): Sdk {
  val homePath = sdk.homePath ?: error("Home path is not passed during fake python sdk creation")
  return PyDetectedSdk(homePath)
}

/**
 * If [sdk] is an "install Python" suggestion ([PySdkToInstall]), downloads and installs Python
 * and returns the freshly detected system-wide SDK, or `null` if the installation failed.
 * Returns `null` if [sdk] is not such a suggestion.
 */
@Suppress("UnstableApiUsage")
internal fun installSdkIfSuggested(sdk: Sdk?): PyDetectedSdk? {
  if (sdk !is PySdkToInstall) return null

  return invokeAndWaitIfNeeded {
    sdk.install(null) {
      detectSystemWideSdks(null, emptyList())
    }.getOrElse {
      LOG.warn(it)
      null
    }
  }
}
