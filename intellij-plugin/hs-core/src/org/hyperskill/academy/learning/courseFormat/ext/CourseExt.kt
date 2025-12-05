@file:JvmName("CourseExt")

package org.hyperskill.academy.learning.courseFormat.ext

import com.intellij.ide.plugins.InstalledPluginsState
import com.intellij.ide.plugins.PluginManager
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.lang.Language
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.compatibility.CourseCompatibility
import org.hyperskill.academy.learning.compatibility.CourseCompatibilityProvider
import org.hyperskill.academy.learning.compatibility.CourseCompatibilityProviderEP
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.EduConfiguratorManager
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.HyperskillCourseAdvertiser

val Course.configurator: EduConfigurator<*>?
  get() {
    val language = languageById ?: return null
    return EduConfiguratorManager.findConfigurator(itemType, environment, language)
  }

val Course.compatibilityProvider: CourseCompatibilityProvider?
  get() {
    return CourseCompatibilityProviderEP.find(languageId, environment)
  }

val Course.sourceDir: String? get() = configurator?.sourceDir
val Course.testDirs: List<String> get() = configurator?.testDirs.orEmpty()

val Course.project: Project?
  get() {
    for (project in ProjectManager.getInstance().openProjects) {
      if (this == StudyTaskManager.getInstance(project).course) {
        return project
      }
    }
    return null
  }

val Course.allTasks: List<Task>
  get() {
    val allTasks = mutableListOf<Task>()
    course.visitTasks { allTasks += it }
    return allTasks
  }

val Course.languageDisplayName: String get() = languageById?.displayName ?: languageId

val Course.technologyName: String?
  get() = compatibilityProvider?.technologyName ?: languageById?.displayName

val Course.supportedTechnologies: List<String>
  get() {
    return when (this) {
      is HyperskillCourseAdvertiser -> this.supportedLanguages
      else -> if (technologyName != null) listOf(technologyName!!) else emptyList()
    }
  }

val Course.tags: List<Tag>
  get() {
    val tags = mutableListOf<Tag>()
    if (course is HyperskillCourseAdvertiser) {
      tags.addAll((this as HyperskillCourseAdvertiser).supportedLanguages.map { ProgrammingLanguageTag(it) })
      tags.add(HumanLanguageTag(humanLanguage))
      return tags
    }

    technologyName?.let { tags.add(ProgrammingLanguageTag(it)) }
    tags.add(HumanLanguageTag(humanLanguage))
    return tags
  }

val Course.languageById: Language?
  get() = Language.findLanguageByID(languageId)


val Course.compatibility: CourseCompatibility
  get() {
    if (this is HyperskillCourseAdvertiser) {
      return CourseCompatibility.Compatible
    }

    return configuratorCompatibility() ?: CourseCompatibility.Compatible
  }

// projectLanguage parameter should be passed only for hyperskill courses because for Hyperskill
// it can differ from the course.programmingLanguage
fun Course.validateLanguage(projectLanguage: String = languageId): Result<Unit, CourseValidationResult> {
  val pluginCompatibility = pluginCompatibility()
  if (pluginCompatibility is CourseCompatibility.PluginsRequired) {
    return Err(PluginsRequired(pluginCompatibility.toInstallOrEnable))
  }

  if (configurator == null) {
    val message = EduCoreBundle.message(
      "rest.service.language.not.supported", ApplicationNamesInfo.getInstance().productName,
      projectLanguage.capitalize()
    )
    return Err(ValidationErrorMessage(message))
  }
  return Ok(Unit)
}

private fun Course.pluginCompatibility(): CourseCompatibility? {
  val requiredPlugins = mutableListOf<PluginInfo>()
  compatibilityProvider?.requiredPlugins()?.let { requiredPlugins.addAll(it) }
  for (pluginInfo in course.pluginDependencies) {
    if (requiredPlugins.find { it.stringId == pluginInfo.stringId } != null) {
      continue
    }
    requiredPlugins.add(pluginInfo)
  }

  if (requiredPlugins.isEmpty()) {
    return null
  }

  val pluginsState = InstalledPluginsState.getInstance()

  // TODO: O(requiredPlugins * allPlugins) because PluginManager.getPlugin takes O(allPlugins).
  //  Can be improved at least to O(requiredPlugins * log(allPlugins))
  val loadedPlugins = PluginManager.getLoadedPlugins()
  val notLoadedPlugins = requiredPlugins
    .mapNotNull {
      if (pluginsState.wasInstalledWithoutRestart(PluginId.getId(it.stringId))) {
        return@mapNotNull null
      }

      val pluginDescriptor = PluginManagerCore.getPlugin(PluginId.getId(it.stringId))
      if (pluginDescriptor == null || pluginDescriptor !in loadedPlugins) {
        it to pluginDescriptor
      }
      else {
        null
      }
    }

  val toInstallOrEnable = notLoadedPlugins.filter { (info, pluginDescriptor) ->
    val pluginId = PluginId.getId(info.stringId)
    // Plugin is just installed and not loaded by IDE (i.e. it requires restart)
    pluginDescriptor == null && !pluginsState.wasInstalled(pluginId) ||
    // Plugin is installed but disabled
    pluginDescriptor != null && PluginManagerCore.isDisabled(pluginId)
  }

  return if (notLoadedPlugins.isNotEmpty()) CourseCompatibility.PluginsRequired(toInstallOrEnable.map { it.first }) else null
}

private fun Course.configuratorCompatibility(): CourseCompatibility? {
  return if (configurator == null) CourseCompatibility.Unsupported else null
}

fun Course.visitEduFiles(visitor: (EduFile) -> Unit) {
  visitTasks { task ->
    for (taskFile in task.taskFiles.values) {
      visitor(taskFile)
    }
  }

  for (additionalFile in additionalFiles) {
    visitor(additionalFile)
  }
}

val Course?.customContentPath: String get() = this?.customContentPath ?: ""