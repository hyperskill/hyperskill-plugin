package com.jetbrains.python.sdk

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.sdk.skeleton.PySkeletonUtil
import java.nio.file.Files
import java.nio.file.Paths

internal fun Project.excludeInnerVirtualEnv(sdk: Sdk) {
  val binary = sdk.homeDirectory ?: return
  ModuleUtil.findModuleForFile(binary, this)?.excludeInnerVirtualEnv(sdk)
}

/**
 * Replacement for `com.jetbrains.python.sdk.findBaseSdks` removed in 262.
 *
 * The original implementation also returned system-wide SDKs from [existingSdks];
 * all call sites in this plugin pass an empty list, so only detection is performed here.
 */
@Suppress("DEPRECATION_ERROR")
fun findBaseSdks(existingSdks: List<Sdk>, module: Module?, context: UserDataHolder): List<Sdk> {
  return detectSystemWideSdks(module, existingSdks, context)
}

/**
 * Replacement for the `com.jetbrains.python.sdk.sdkSeemsValid` extension, which is not available in 262.
 */
val Sdk.sdkSeemsValid: Boolean
  get() = isSdkSeemsValid

/**
 * In 262 installable Python interpreters are no longer SDK instances and their API is internal.
 * Adapt them to the SDK-based course wizard while keeping all access to the internal API in the
 * Java bridge (Java does not enforce Kotlin's module-level `internal` visibility).
 */
fun getSdksToInstall(): List<Sdk> = PythonSdkInstallBridge.getSuggestions().map(::PySdkToInstallCompat)

internal class PySdkToInstallCompat(
  val suggestion: PythonSdkInstallBridge.Suggestion
) : ProjectJdkImpl(
  suggestion.name,
  PythonSdkType.getInstance(),
  null,
  suggestion.version,
)

internal fun Sdk.adminPermissionsNeeded(): Boolean {
  val pathToCheck = sitePackagesDirectory?.path ?: homePath ?: return false
  return !Files.isWritable(Paths.get(pathToCheck))
}

private val Sdk.sitePackagesDirectory: VirtualFile?
  get() = PySkeletonUtil.getSitePackagesDirectory(this)
