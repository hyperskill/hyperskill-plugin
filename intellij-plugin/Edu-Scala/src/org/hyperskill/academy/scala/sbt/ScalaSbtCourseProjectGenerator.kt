package org.hyperskill.academy.scala.sbt

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.EduNames.PROJECT_NAME
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.gradleSanitizeName
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.scala.sbt.ScalaSbtCourseBuilder.Companion.BUILD_SBT
import org.jetbrains.plugins.scala.project.Version
import org.jetbrains.sbt.Sbt
import org.jetbrains.sbt.SbtVersion
import org.jetbrains.sbt.project.SbtProjectSystem
import org.jetbrains.sbt.project.settings.SbtProjectSettings

class ScalaSbtCourseProjectGenerator(builder: ScalaSbtCourseBuilder, course: Course) : CourseProjectGenerator<JdkProjectSettings>(
  builder,
  course
) {

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    val sbtVersion = maxOf(SbtVersion.`Latest$`.`MODULE$`.Sbt_1().value(), MIN_RECOMMENDED_SBT_VERSION)
    val templateVariables = mapOf(
      PROJECT_NAME to gradleSanitizeName(holder.courseDir.name),
      "SBT_VERSION" to sbtVersion.toString()
    )

    return listOf(
      GeneratorUtils.createFromInternalTemplateOrFromDisk(holder.courseDir, BUILD_SBT, BUILD_SBT, templateVariables),
      GeneratorUtils.createFromInternalTemplateOrFromDisk(
        holder.courseDir,
        "${Sbt.ProjectDirectory()}/${Sbt.PropertiesFile()}",
        Sbt.PropertiesFile(),
        templateVariables
      )
    )
  }

  override suspend fun prepareToOpen(project: Project, module: Module) {
    super.prepareToOpen(project, module)
    @Suppress("UnstableApiUsage")
    writeAction { GeneratorUtils.removeModule(project, module) }
    project.putUserData(ExternalSystemDataKeys.NEWLY_IMPORTED_PROJECT, true)
  }

  override fun afterProjectGenerated(
    project: Project,
    projectSettings: JdkProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    projectSettings.setUpProjectJdk(project, course)
    setupSbtSettings(project)
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }

  private fun setupSbtSettings(project: Project) {
    val location = project.basePath ?: error("Failed to find base path for the project during scala sbt setup")
    val systemSettings = ExternalSystemApiUtil.getSettings(project, SbtProjectSystem.Id)

    val projectSettings = SbtProjectSettings()
    projectSettings.externalProjectPath = location

    val projects = systemSettings.linkedProjectsSettings.toHashSet()
    projects.add(projectSettings)
    systemSettings.linkedProjectsSettings = projects
  }

  companion object {
    // Minimal version of sbt that supports java 13
    private val MIN_RECOMMENDED_SBT_VERSION: Version = Version("1.3.3")
  }
}
