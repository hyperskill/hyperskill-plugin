package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.sdk.PyDetectedSdk

/**
 * TODO: 262 — `PySdkToInstall` and `getSdksToInstall()` became internal in the Python plugin,
 * so "install Python" suggestions are never offered on this platform
 * (see the `getSdksToInstall` compat shim in `com.jetbrains.python.sdk`)
 * and there is never anything to install here.
 */
internal fun installSdkIfSuggested(@Suppress("UNUSED_PARAMETER") sdk: Sdk?): PyDetectedSdk? = null
