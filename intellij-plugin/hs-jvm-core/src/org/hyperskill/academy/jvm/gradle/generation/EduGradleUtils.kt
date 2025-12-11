package org.hyperskill.academy.jvm.gradle.generation

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_INTERNAL_JAVA
import com.intellij.openapi.externalSystem.service.execution.ExternalSystemJdkUtil.USE_PROJECT_JDK
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.JavaSdkVersion
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFileManager
import org.hyperskill.academy.jvm.gradle.GradleWrapperListener
import org.hyperskill.academy.jvm.messages.EduJVMBundle
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.computeUnderProgress
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.createFromInternalTemplateOrFromDisk
import org.hyperskill.academy.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import org.jetbrains.plugins.gradle.settings.DistributionType
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.io.IOException
import java.util.*

object EduGradleUtils {
  fun isConfiguredWithGradle(project: Project): Boolean {
    return hasDefaultGradleScriptFile(project) || hasDefaultGradleKtsScriptFile(project)
  }

  private fun hasDefaultGradleScriptFile(project: Project): Boolean {
    return File(project.basePath, GradleConstants.DEFAULT_SCRIPT_NAME).exists()
  }

  private fun hasDefaultGradleKtsScriptFile(project: Project): Boolean {
    return File(project.basePath, GradleConstants.KOTLIN_DSL_SCRIPT_NAME).exists()
  }

  fun hasCourseHaveGradleKtsFiles(course: Course): Boolean =
    course.additionalFiles.find { it.name == GradleConstants.KOTLIN_DSL_SCRIPT_NAME } != null &&
    course.additionalFiles.find { it.name == GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME } != null

  @Throws(IOException::class)
  fun createProjectGradleFiles(
    holder: CourseInfoHolder<Course>,
    templates: Map<String, String>,
    templateVariables: Map<String, Any>
  ): List<EduFile> =
    templates.map { (name, templateName) ->
      createFromInternalTemplateOrFromDisk(holder.courseDir, name, templateName, templateVariables)
    }

  fun setGradleSettings(project: Project, sdk: Sdk?, location: String, distributionType: DistributionType = DistributionType.WRAPPED) {
    val systemSettings = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID)
    val existingProject = ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).getLinkedProjectSettings(location)
    if (existingProject is GradleProjectSettings) {
      if (existingProject.distributionType == null) {
        existingProject.distributionType = distributionType
      }
      if (existingProject.externalProjectPath == null) {
        existingProject.externalProjectPath = location
      }
      setUpGradleJvm(project, existingProject, sdk)
      return
    }

    val gradleProjectSettings = GradleProjectSettings()
    gradleProjectSettings.distributionType = distributionType
    gradleProjectSettings.externalProjectPath = location
    // IDEA runner is much more faster and it doesn't write redundant messages into console.
    // Note, it doesn't affect tests - they still are run with gradle runner
    gradleProjectSettings.delegatedBuild = false
    setUpGradleJvm(project, gradleProjectSettings, sdk)

    val projects = systemSettings.linkedProjectsSettings.toHashSet()
    projects.add(gradleProjectSettings)
    systemSettings.linkedProjectsSettings = projects
  }

  private fun setUpGradleJvm(project: Project, projectSettings: GradleProjectSettings, sdk: Sdk?) {
    if (sdk == null) return

    val gradleVersion = getGradleVersion(project)
    val maxCompatibleJdk = gradleVersion?.let { getMaxCompatibleJdkVersion(it) }

    // If we know Gradle version and max compatible JDK, try to find a compatible JDK
    if (maxCompatibleJdk != null) {
      val compatibleJdk = findCompatibleJdk(maxCompatibleJdk)
      if (compatibleJdk != null) {
        projectSettings.gradleJvm = compatibleJdk.name
        return
      }
    }

    // Fallback to original logic
    val projectSdkVersion = sdk.javaSdkVersion
    val internalSdkVersion = computeUnderProgress(project, EduJVMBundle.message("progress.resolving.suitable.jdk"), false) {
      ExternalSystemJdkUtil.resolveJdkName(null, USE_INTERNAL_JAVA)
    }?.javaSdkVersion

    // Try to avoid incompatibility between Gradle and JDK versions
    projectSettings.gradleJvm = when {
      internalSdkVersion == null -> USE_PROJECT_JDK
      projectSdkVersion == null -> USE_INTERNAL_JAVA
      else -> if (internalSdkVersion < projectSdkVersion) USE_INTERNAL_JAVA else USE_PROJECT_JDK
    }
  }

  /**
   * Reads Gradle version from gradle-wrapper.properties or .gradle cache directory.
   */
  private fun getGradleVersion(project: Project): String? {
    val basePath = project.basePath ?: return null

    // Try to read from gradle-wrapper.properties
    val wrapperPropertiesFile = File(basePath, "gradle/wrapper/gradle-wrapper.properties")
    if (wrapperPropertiesFile.exists()) {
      try {
        val properties = Properties()
        wrapperPropertiesFile.inputStream().use { properties.load(it) }
        val distributionUrl = properties.getProperty("distributionUrl")
        if (distributionUrl != null) {
          // Extract version from URL like: https://services.gradle.org/distributions/gradle-8.5-bin.zip
          val versionRegex = Regex("gradle-(\\d+\\.\\d+(?:\\.\\d+)?)")
          versionRegex.find(distributionUrl)?.groupValues?.get(1)?.let { return it }
        }
      }
      catch (_: Exception) {
        // Continue to try other methods
      }
    }

    // Try to detect version from .gradle cache directory
    val gradleCacheDir = File(basePath, ".gradle")
    if (gradleCacheDir.exists() && gradleCacheDir.isDirectory) {
      val versionRegex = Regex("^(\\d+\\.\\d+(?:\\.\\d+)?)$")
      gradleCacheDir.listFiles()
        ?.filter { it.isDirectory }
        ?.mapNotNull { versionRegex.find(it.name)?.groupValues?.get(1) }
        ?.maxWithOrNull(GradleVersionComparator)
        ?.let { return it }
    }

    return null
  }

  private object GradleVersionComparator : Comparator<String> {
    override fun compare(v1: String, v2: String): Int {
      val parts1 = v1.split(".").mapNotNull { it.toIntOrNull() }
      val parts2 = v2.split(".").mapNotNull { it.toIntOrNull() }
      for (i in 0 until maxOf(parts1.size, parts2.size)) {
        val p1 = parts1.getOrElse(i) { 0 }
        val p2 = parts2.getOrElse(i) { 0 }
        if (p1 != p2) return p1.compareTo(p2)
      }
      return 0
    }
  }

  /**
   * Returns maximum JDK version compatible with the given Gradle version.
   * Based on https://docs.gradle.org/current/userguide/compatibility.html
   */
  private fun getMaxCompatibleJdkVersion(gradleVersion: String): JavaSdkVersion? {
    val parts = gradleVersion.split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0

    return when {
      major >= 9 -> JavaSdkVersion.JDK_23  // Gradle 9.x supports JDK 23
      major >= 8 && minor >= 10 -> JavaSdkVersion.JDK_23
      major >= 8 && minor >= 8 -> JavaSdkVersion.JDK_22
      major >= 8 && minor >= 5 -> JavaSdkVersion.JDK_21
      major >= 8 && minor >= 3 -> JavaSdkVersion.JDK_20
      major >= 8 -> JavaSdkVersion.JDK_19
      major >= 7 && minor >= 6 -> JavaSdkVersion.JDK_19
      major >= 7 && minor >= 5 -> JavaSdkVersion.JDK_18
      major >= 7 && minor >= 3 -> JavaSdkVersion.JDK_17
      major >= 7 -> JavaSdkVersion.JDK_16
      else -> JavaSdkVersion.JDK_11
    }
  }

  /**
   * Finds the highest available JDK that is compatible with the given max version.
   */
  private fun findCompatibleJdk(maxVersion: JavaSdkVersion): Sdk? {
    val javaSdk = JavaSdk.getInstance()
    return ProjectJdkTable.getInstance().allJdks
      .filter { javaSdk.isOfVersionOrHigher(it, JavaSdkVersion.JDK_1_8) }
      .mapNotNull { sdk -> javaSdk.getVersion(sdk)?.let { version -> sdk to version } }
      .filter { (_, version) -> version <= maxVersion }
      .maxByOrNull { (_, version) -> version.ordinal }
      ?.first
  }

  private val Sdk.javaSdkVersion: JavaSdkVersion? get() = JavaSdk.getInstance().getVersion(this)

  fun updateGradleSettings(project: Project) {
    val projectBasePath = project.basePath ?: error("Failed to find base path for the project during gradle project setup")
    val sdk = ProjectRootManager.getInstance(project).projectSdk
    setGradleSettings(project, sdk, projectBasePath)
  }

  fun setupGradleProject(project: Project) {
    val projectBasePath = project.basePath
    if (projectBasePath != null) {
      // Android Studio creates non executable `gradlew`
      val gradlew = File(FileUtil.toSystemDependentName(projectBasePath), GRADLE_WRAPPER_UNIX)
      if (gradlew.exists()) {
        gradlew.setExecutable(true)
      }
      else {
        val taskManager = StudyTaskManager.getInstance(project)
        val connection = ApplicationManager.getApplication().messageBus.connect(taskManager)
        connection.subscribe(VirtualFileManager.VFS_CHANGES, GradleWrapperListener(connection))
      }
    }
  }
}
