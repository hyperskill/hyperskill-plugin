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
import com.intellij.util.lang.JavaVersion
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
    // `setGradleSettings` is called on every project opening, so a non-empty `gradleJvm` here is either
    // the value we picked before or the one the user chose explicitly. Overwriting it changes the JVM
    // the Gradle daemon runs on, and with it `JavaVersion.current()`, which the generated Hyperskill
    // build scripts use to compute the requested Java toolchain.
    if (!projectSettings.gradleJvm.isNullOrBlank()) return

    val gradleVersion = getGradleVersion(project)
    val maxCompatibleJdk = gradleVersion?.let { getMaxCompatibleJdkFeatureVersion(it) }

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
      ExternalSystemJdkUtil.resolveJdkName(null as Sdk?, USE_INTERNAL_JAVA)
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
   * Returns the maximum JDK feature version compatible with the given Gradle version,
   * or `null` for a Gradle version newer than the table below: guessing there would pin the daemon
   * to an outdated JDK, so it's better to leave the choice to the platform.
   *
   * Based on https://docs.gradle.org/current/userguide/compatibility.html
   */
  private fun getMaxCompatibleJdkFeatureVersion(gradleVersion: String): Int? {
    val parts = gradleVersion.split(".")
    val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
    val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0

    return when {
      major > 9 -> null
      major == 9 && minor >= 1 -> 25
      major == 9 -> 24
      major == 8 && minor >= 14 -> 24
      major == 8 && minor >= 10 -> 23
      major == 8 && minor >= 8 -> 22
      major == 8 && minor >= 5 -> 21
      major == 8 && minor >= 3 -> 20
      major == 8 -> 19
      major == 7 && minor >= 6 -> 19
      major == 7 && minor >= 5 -> 18
      major == 7 && minor >= 3 -> 17
      major == 7 -> 16
      else -> 11
    }
  }

  /**
   * Finds the highest available release JDK that is compatible with the given max feature version.
   */
  private fun findCompatibleJdk(maxFeatureVersion: Int): Sdk? {
    return ProjectJdkTable.getInstance().getSdksOfType(JavaSdk.getInstance())
      .mapNotNull { sdk -> sdk.releaseFeatureVersion?.let { sdk to it } }
      .filter { (_, featureVersion) -> featureVersion in MIN_SUPPORTED_JDK_FEATURE_VERSION..maxFeatureVersion }
      .maxByOrNull { (_, featureVersion) -> featureVersion }
      ?.first
  }

  /**
   * Feature version of a JDK, or `null` if it is a pre-release build.
   *
   * The Gradle integration refuses to run on EA and project builds (`26-ea`, `23-valhalla`, ...) and falls back
   * to an arbitrary installation instead, so such JDKs must never be offered as the Gradle JVM.
   *
   * Note that [JavaSdk.getVersion] is deliberately not used here: `JavaSdkVersion` has no entry for a JDK newer
   * than the one the IDE knows about, so it reports `null` for it, and such a JDK would be silently skipped.
   */
  private val Sdk.releaseFeatureVersion: Int?
    get() {
      val version = versionString ?: return null
      if (PRE_RELEASE_JDK_VERSION.containsMatchIn(version)) return null
      val javaVersion = JavaVersion.tryParse(version) ?: return null
      return if (javaVersion.ea) null else javaVersion.feature
    }

  private val Sdk.javaSdkVersion: JavaSdkVersion? get() = JavaSdk.getInstance().getVersion(this)

  private const val MIN_SUPPORTED_JDK_FEATURE_VERSION = 8

  /** Matches a feature version followed by a pre-release qualifier: `26-ea`, `25-internal`, `23-valhalla`. */
  private val PRE_RELEASE_JDK_VERSION = Regex("""\d+(\.\d+)*-[A-Za-z]""")

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
