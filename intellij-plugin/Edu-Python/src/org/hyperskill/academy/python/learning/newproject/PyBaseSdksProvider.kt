package org.hyperskill.academy.python.learning.newproject

import com.intellij.openapi.util.UserDataHolder
import com.intellij.util.concurrency.annotations.RequiresBackgroundThread
import com.intellij.util.concurrency.annotations.RequiresReadLockAbsence
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor.getLanguageLevelFromVersionStringStatic
import com.jetbrains.python.sdk.flavors.PythonSdkFlavor.getVersionStringStatic
import kotlin.io.path.pathString

/**
 * The idea of this class is to reuse information about sdk version and python language level
 * if it is already computed
 */
object PyBaseSdksProvider {
  @Volatile
  private var sdkDescriptors: Map<String, PyBaseSdkDescriptor> = emptyMap()

  @Suppress("UnstableApiUsage")
  @RequiresBackgroundThread
  @RequiresReadLockAbsence
  fun getBaseSdks(context: UserDataHolder? = null): Collection<PyBaseSdkDescriptor> {
    val oldDescriptors = sdkDescriptors
    val newDescriptors = mutableMapOf<String, PyBaseSdkDescriptor>()

    val flavor = PythonSdkFlavor.getApplicableFlavors(false).first()
    val sdkPaths = flavor.suggestLocalHomePaths(null, context).map { it.pathString }
    for (sdkPath in sdkPaths) {
      val sdkDescriptor = oldDescriptors[sdkPath]
      if (sdkDescriptor != null) {
        newDescriptors[sdkPath] = sdkDescriptor
        continue
      }
      val versionString = getVersionStringStatic(sdkPath) ?: continue
      val languageLevel = getLanguageLevelFromVersionStringStatic(versionString)
      newDescriptors[sdkPath] = PyBaseSdkDescriptor(sdkPath, versionString, languageLevel)
    }

    sdkDescriptors = newDescriptors
    return newDescriptors.values
  }
}

class PyBaseSdkDescriptor(val path: String, val version: String, val languageLevel: LanguageLevel)
