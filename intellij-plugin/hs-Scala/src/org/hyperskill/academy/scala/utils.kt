package org.hyperskill.academy.scala

import com.intellij.util.text.VersionComparatorUtil
import org.hyperskill.academy.learning.pluginVersion

val isScalaPluginCompatible: Boolean
  get() {
    val scalaPluginVersion = pluginVersion("org.intellij.scala") ?: return false
    return VersionComparatorUtil.compare(scalaPluginVersion, "2021.3.6") >= 0
  }
