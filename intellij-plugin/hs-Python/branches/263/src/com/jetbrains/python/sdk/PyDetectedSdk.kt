package com.jetbrains.python.sdk

import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl

/**
 * Replacement for `com.jetbrains.python.sdk.PyDetectedSdk` removed from the Python plugin in 263:
 * an interpreter found on disk that is not registered as an SDK yet.
 *
 * Mirrors the removed class: both the name and the home path are the interpreter path, and the version is unknown
 * until the SDK is set up (see `PyCourseProjectGenerator.updateSdkIfNeeded`).
 */
class PyDetectedSdk(name: String) : ProjectJdkImpl(name, PythonSdkType.getInstance(), name, "") {
  override fun getVersionString(): String = ""
}
