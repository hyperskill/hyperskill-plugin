package org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers

import com.intellij.openapi.util.SystemInfo
import org.hyperskill.academy.learning.messages.EduPropertiesBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE_NAME = "style.browser"

object TaskToolWindowBundle : EduPropertiesBundle(BUNDLE_NAME) {

  fun value(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String): String {
    return valueOrEmpty(key)
  }

  fun getFloatParameter(@PropertyKey(resourceBundle = BUNDLE_NAME) key: String) = value(
    if (SystemInfo.isMac) "mac.$key" else key
  ).toFloat()

  fun getOsDependentParameter(key: String) = value(parameterNameWithOSPrefix(key))

  private fun parameterNameWithOSPrefix(name: String): String {
    return when {
      SystemInfo.isMac -> "mac.$name"
      SystemInfo.isWindows -> "win.$name"
      else -> "linux.$name"
    }
  }
}