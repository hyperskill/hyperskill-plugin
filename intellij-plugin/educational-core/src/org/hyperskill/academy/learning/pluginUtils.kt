@file:JvmName("PluginUtils")

package org.hyperskill.academy.learning

import com.intellij.externalDependencies.DependencyOnPlugin
import com.intellij.externalDependencies.ExternalDependenciesManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.updateSettings.impl.pluginsAdvertisement.installAndEnable
import com.intellij.util.text.VersionComparatorUtil
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.compatibilityProvider
import org.jsoup.Jsoup

const val KOTLIN_PLUGIN_ID = "org.jetbrains.kotlin"
val DEFAULT_KOTLIN_VERSION = KotlinVersion("1.9.0")
private val KOTLIN_VERSION_PATTERN = """(\d+-)((?<version>\d+\.\d+(.\d+)?(-(RC|RC2|M1|M2))?)(-release-\d+)?).*""".toRegex()

fun pluginVersion(pluginId: String): String? = PluginManagerCore.getPlugin(PluginId.getId(pluginId))?.version

// Kotlin plugin keeps the latest supported language version only in change notes
fun kotlinVersionFromPlugin(pluginId: String): String? {
  val changeNotes = PluginManagerCore.getPlugin(PluginId.getId(pluginId))?.changeNotes ?: return null
  return Jsoup.parse(changeNotes).getElementsByTag("h3").firstOrNull()?.text()
}

fun kotlinVersion(): KotlinVersion {
  val kotlinPluginVersion = kotlinVersionFromPlugin(KOTLIN_PLUGIN_ID) ?: return DEFAULT_KOTLIN_VERSION
  val matchResult = KOTLIN_VERSION_PATTERN.matchEntire(kotlinPluginVersion) ?: return DEFAULT_KOTLIN_VERSION
  val version = matchResult.groups["version"]?.value ?: return DEFAULT_KOTLIN_VERSION
  val kotlinVersion = KotlinVersion(version)
  return maxOf(kotlinVersion, DEFAULT_KOTLIN_VERSION)
}

data class KotlinVersion(val version: String) : Comparable<KotlinVersion> {
  override fun compareTo(other: KotlinVersion): Int = VersionComparatorUtil.compare(version, other.version)
}

fun setUpPluginDependencies(project: Project, course: Course) {
  val allDependencies = course.pluginDependencies.map { DependencyOnPlugin(it.stringId, it.minVersion, it.maxVersion) }.toMutableList()

  course.compatibilityProvider?.requiredPlugins()?.forEach { plugin ->
    if (allDependencies.none { plugin.stringId == it.pluginId }) {
      allDependencies.add(DependencyOnPlugin(plugin.stringId, null, null))
    }
  }

  ExternalDependenciesManager.getInstance(project).setAllDependencies(allDependencies)
}

fun installAndEnablePlugin(pluginIds: Set<PluginId>, onSuccess: Runnable) = installAndEnable(null, pluginIds, true, onSuccess = onSuccess)

val TIME_LOGGER = Logger.getInstance("JetBrainsAcademy.performance.measure")

fun <T> measureTimeAndLog(title: String, block: () -> T): T {
  val start = System.currentTimeMillis()
  val value = block()
  val time = System.currentTimeMillis() - start
  TIME_LOGGER.info("$title: $time ms")
  return value
}