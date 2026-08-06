package com.jetbrains.python.sdk

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.runBlockingMaybeCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.impl.ProjectJdkImpl
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.sdk.flavors.PyFlavorAndData
import com.jetbrains.python.sdk.flavors.PyFlavorData
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import com.jetbrains.python.sdk.skeleton.PySkeletonUtil
import org.hyperskill.academy.python.learning.newproject.FakePythonSdkFlavor
import java.lang.reflect.InvocationTargetException
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

private val LOG = Logger.getInstance("org.hyperskill.academy.python.sdk.compat")

internal fun Project.excludeInnerVirtualEnv(sdk: Sdk) {
  val binary = sdk.homeDirectory ?: return
  ModuleUtil.findModuleForFile(binary, this)?.excludeInnerVirtualEnv(sdk)
}

/**
 * Replacement for `com.jetbrains.python.sdk.findBaseSdks` removed in 262.
 *
 * The original implementation also returned system-wide SDKs from [existingSdks];
 * all call sites in this plugin pass an empty list, so only detection is performed here.
 *
 * Interpreters come from `SystemPythonService` — the API the Python plugin's own interpreter lists
 * are built on in 262. The flavor-based [detectSystemWideSdks] it replaced is kept as a fallback:
 * it still works in IDEA, but misses locally installed interpreters in PyCharm.
 */
fun findBaseSdks(existingSdks: List<Sdk>, module: Module?, context: UserDataHolder): List<Sdk> {
  val systemPythons = findSystemPythonPaths()
  LOG.info("findBaseSdks: SystemPythonService found ${systemPythons.size} interpreters: $systemPythons")

  val managerPythons = findPythonInstallManagerPaths()
  LOG.info("findBaseSdks: Python install manager locations: $managerPythons")

  val allPaths = (systemPythons + managerPythons).distinctBy { it.lowercase() }
  if (allPaths.isNotEmpty()) {
    return allPaths.map(::PyDetectedSdk)
  }

  LOG.info("findBaseSdks: nothing found, falling back to flavor-based detection")
  @Suppress("DEPRECATION_ERROR")
  return detectSystemWideSdks(module, existingSdks, context)
}

/**
 * Interpreters installed by the Python install manager — the default python.org Windows installer since 3.14.
 * It keeps runtimes under `%LOCALAPPDATA%\Python\<runtime>\python.exe` and does not write the PEP 514
 * `InstallPath` registry keys, which are the only thing `SystemPythonService` reads on Windows (besides
 * the Windows Store), so such installations have to be collected from disk directly.
 */
private fun findPythonInstallManagerPaths(): List<String> {
  if (!SystemInfo.isWindows) return emptyList()
  val localAppData = System.getenv("LOCALAPPDATA") ?: return emptyList()
  val root = Paths.get(localAppData, "Python")
  if (!Files.isDirectory(root)) return emptyList()
  return try {
    Files.list(root).use { children ->
      children
        // `bin` contains launcher shims for the default runtime, which is already listed by its own directory
        .filter { Files.isDirectory(it) && it.fileName.toString() != "bin" }
        .map { it.resolve("python.exe") }
        .filter { Files.isExecutable(it) }
        .map { it.toString() }
        .toList()
    }
  }
  catch (e: Exception) {
    LOG.warn("findPythonInstallManagerPaths: failed to list $root", e)
    emptyList()
  }
}

private const val SYSTEM_PYTHON_MODULE = "intellij.python.community.services.systemPython"

/**
 * Returns binary paths of the local interpreters known to the Python plugin's `SystemPythonService`.
 *
 * The service lives in the [SYSTEM_PYTHON_MODULE] content module marked `visibility="internal"`, and the plugin
 * loader rejects descriptor `<module>` dependencies on internal modules from other vendors' plugins.
 * So instead of a compile-time dependency, the service is reached through the classloader of its module.
 */
private fun findSystemPythonPaths(): List<String> {
  try {
    val moduleDescriptor = PluginManagerCore.getPluginSet().getUnsortedEnabledModules()
      .firstOrNull { it.moduleId.name == SYSTEM_PYTHON_MODULE }
    if (moduleDescriptor == null) {
      LOG.warn("findSystemPythonPaths: $SYSTEM_PYTHON_MODULE module is not enabled")
      return emptyList()
    }
    val loader = moduleDescriptor.pluginClassLoader
    if (loader == null) {
      LOG.warn("findSystemPythonPaths: $SYSTEM_PYTHON_MODULE module has no classloader")
      return emptyList()
    }

    val serviceInterface = Class.forName("com.intellij.python.community.services.systemPython.SystemPythonService", true, loader)
    val service = Class.forName("com.intellij.python.community.services.systemPython.ApiKt", true, loader)
      .getMethod("SystemPythonService")
      .invoke(null)

    // `suspend fun findSystemPythons(eelApi: EelApi = localEel, forceRefresh: Boolean = false): List<SystemPython>`,
    // invoked through the compiler-generated default-arguments bridge: mask 0b11 applies both default values
    val findSystemPythons = serviceInterface.methods.single { it.name == "findSystemPythons\$default" }
    val pythons = runBlockingMaybeCancellable {
      suspendCoroutineUninterceptedOrReturn<Any?> { continuation ->
        findSystemPythons.invoke(null, service, null, false, continuation, 0b11, null)
      } as List<*>
    }

    return pythons.filterNotNull().map { python ->
      python.javaClass.getMethod("getPythonBinary").invoke(python).toString()
    }
  }
  catch (e: ProcessCanceledException) {
    throw e
  }
  catch (e: Throwable) {
    // A reflective call wraps exceptions thrown before the first suspension in InvocationTargetException,
    // so cancellation has to be unwrapped before it can be rethrown
    val cause = (e as? InvocationTargetException)?.cause ?: e
    if (cause is ProcessCanceledException) throw cause
    LOG.warn("findSystemPythonPaths: SystemPythonService is not available", cause)
    return emptyList()
  }
}

/**
 * Replacement for the `com.jetbrains.python.sdk.sdkSeemsValid` extension, which is not available in 262.
 *
 * Its 262 counterpart [isSdkSeemsValid] requires [PythonSdkAdditionalData] and throws an exception without it
 * ("... doesn't have an additional data: it was created by buggy code"), while a just detected interpreter has none:
 * in 261 the missing data was created on the fly instead. Such an interpreter is validated by its flavor,
 * which is what the check comes down to anyway.
 */
val Sdk.sdkSeemsValid: Boolean
  get() {
    if (sdkAdditionalData is PythonSdkAdditionalData) return isSdkSeemsValid
    val binaryPath = homePath?.takeIf { it.isNotBlank() } ?: return false
    return PythonSdkFlavor.getFlavor(binaryPath) != null
  }

/**
 * In 262 installable Python interpreters are no longer SDK instances and their API is internal.
 * Adapt them to the SDK-based course wizard while keeping all access to the internal API in the
 * Java bridge (Java does not enforce Kotlin's module-level `internal` visibility).
 */
fun getSdksToInstall(): List<Sdk> = PythonSdkInstallBridge.getSuggestions().map { PySdkToInstallCompat.create(it) }

internal class PySdkToInstallCompat private constructor(
  val suggestion: PythonSdkInstallBridge.Suggestion
) : ProjectJdkImpl(
  suggestion.name,
  PythonSdkType.getInstance(),
  // Python is not installed yet, so there is no home path.
  // `null` is rejected by the workspace model since 262 (`SdkBridgeImpl.createEmptySdkEntity` requires a non-null path),
  // and an empty string is what the platform itself passes for a home-less SDK in `ProjectJdkImpl(name, sdkType)`.
  "",
  suggestion.version,
) {

  companion object {
    fun create(suggestion: PythonSdkInstallBridge.Suggestion): PySdkToInstallCompat {
      val sdk = PySdkToInstallCompat(suggestion)
      // Without additional data the Python plugin detects the flavor by the home path, which fails for an empty one,
      // and reports the sdk as created by buggy code on every access. Same trick as in `PySdkToCreateVirtualEnv`.
      with(sdk.sdkModificator) {
        sdkAdditionalData = PythonSdkAdditionalData(PyFlavorAndData(PyFlavorData.Empty, FakePythonSdkFlavor))
        // This sdk is not associated with anything yet, so it's ok not to use a write action here.
        // Otherwise, we have to switch to EDT since this code is invoked from BGT of a modal dialog
        applyChangesWithoutWriteAction()
      }
      return sdk
    }
  }
}

internal fun Sdk.adminPermissionsNeeded(): Boolean {
  val pathToCheck = sitePackagesDirectory?.path ?: homePath ?: return false
  return !Files.isWritable(Paths.get(pathToCheck))
}

private val Sdk.sitePackagesDirectory: VirtualFile?
  get() = PySkeletonUtil.getSitePackagesDirectory(this)
