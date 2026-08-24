package org.hyperskill.academy.jvm.gradle

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils.setupGradleProject
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils.updateGradleSettings
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.RefreshCause
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.IOException
import kotlin.coroutines.resume

class GradleStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    if (project.isDisposed || !project.isEduProject()) {
      return
    }
    if (EduGradleUtils.isConfiguredWithGradle(project)) {
      val buildScriptMigrated = migrateScript(project, GradleConstants.DEFAULT_SCRIPT_NAME, ::migrateLegacyUtilSourceSetReferences)
      val settingsMigrated = migrateScript(project, GradleConstants.SETTINGS_FILE_NAME, ::addToolchainResolver)
      updateGradleSettings(project)
      if (buildScriptMigrated || settingsMigrated) {
        // The import triggered by project opening has already read the outdated script,
        // so it has to be re-run to pick up the migrated one.
        GradleCourseRefresher.firstAvailable()?.refresh(project, RefreshCause.DEPENDENCIES_UPDATED)
      }
    }

    // Convert DumbService.runWhenSmart to a suspending function
    suspendCancellableCoroutine { continuation ->
      DumbService.getInstance(project).runWhenSmart {
        val taskManager = StudyTaskManager.getInstance(project)
        val course = taskManager.course
        if (course == null) {
          LOG.warn("Opened project is with null course")
          continuation.resume(Unit)
          return@runWhenSmart
        }

        if (EduGradleUtils.isConfiguredWithGradle(project)) {
          setupGradleProject(project)
        }

        // Ensure util module directory exists for Hyperskill projects.
        // Gradle 9.x requires module directories to exist during project configuration.
        if (course is HyperskillCourse) {
          ensureUtilModuleDirectoryExists(project)
        }

        continuation.resume(Unit)
      }
    }
  }

  /**
   * Applies [migration] to the Gradle script named [scriptName] in the project root.
   *
   * @return `true` if the script was actually changed
   */
  private suspend fun migrateScript(project: Project, scriptName: String, migration: (String) -> String): Boolean {
    val projectDir = project.guessProjectDir() ?: return false
    val scriptFile = projectDir.findChild(scriptName) ?: return false
    if (scriptFile.isDirectory) return false

    return try {
      val originalContent = VfsUtilCore.loadText(scriptFile)
      val migratedContent = migration(originalContent)
      if (migratedContent == originalContent) return false

      writeAction {
        VfsUtil.saveText(scriptFile, migratedContent)
      }
      LOG.info("Migrated ${scriptFile.path}")
      true
    }
    catch (e: IOException) {
      LOG.warn("Failed to migrate ${scriptFile.path}", e)
      false
    }
  }

  private fun ensureUtilModuleDirectoryExists(project: Project) {
    val projectDir = project.guessProjectDir() ?: return
    val utilDir = projectDir.findChild(UTIL_MODULE_NAME)
    if (utilDir != null && utilDir.isDirectory) return

    runWriteAction {
      VfsUtil.createDirectoryIfMissing(projectDir, "$UTIL_MODULE_NAME/src")
    }
  }

  companion object {
    private val LOG = Logger.getInstance(GradleStartupActivity::class.java)

    private const val UTIL_MODULE_NAME = "util"

    // The negative lookbehind also makes the replacement idempotent: an already migrated
    // `rootProject.project(':util')` is preceded by a dot and is not matched again
    private val LEGACY_UTIL_SOURCE_SET_REFERENCE =
      Regex("""(?<![\w.])project\((['"]):util\1\)\.sourceSets\.(?:main|test)\.output""")

    /**
     * Rewrites `project(':util').sourceSets.*.output` references left in build scripts of old projects.
     *
     * Inside a `dependencies { }` block such a call is resolved against `DependencyHandler`,
     * which since Gradle 9 provides its own `project(String)` returning a `ProjectDependency` instead of a `Project`,
     * so `sourceSets` is no longer resolvable there. Qualifying the call with `rootProject` makes it resolve
     * against `Project` again, which is valid for every Gradle version, so the migration is not tied to a
     * particular IDE or Gradle version.
     */
    @VisibleForTesting
    fun migrateLegacyUtilSourceSetReferences(content: String): String =
      content.replace(LEGACY_UTIL_SOURCE_SET_REFERENCE) { matchResult -> "rootProject.${matchResult.value}" }

    private const val FOOJAY_RESOLVER_ID = "org.gradle.toolchains.foojay-resolver-convention"
    private const val FOOJAY_RESOLVER_VERSION = "1.0.0"

    /** Matches the `hs-gradle-plugin` classpath entry that only Hyperskill settings scripts contain */
    private const val HS_GRADLE_PLUGIN = "hs-gradle-plugin"

    private val BUILD_SCRIPT_BLOCK_START = Regex("""(?m)^\s*buildscript\s*\{""")

    /** Leading blank line separates the inserted block from the `buildscript { }` block above it */
    private val TOOLCHAIN_RESOLVER_BLOCK = """
      |
      |
      |plugins {
      |  id '$FOOJAY_RESOLVER_ID' version '$FOOJAY_RESOLVER_VERSION'
      |}
    """.trimMargin()

    /**
     * Adds the Foojay toolchain resolver to `settings.gradle` of already generated Hyperskill projects.
     *
     * The generated build script requests a Java toolchain of `max(<Gradle daemon JVM>, hs.java.version)`.
     * Without a resolver Gradle cannot provision that JDK, so the build fails with
     * "Toolchain download repositories have not been configured" whenever it is not installed locally.
     *
     * Only scripts generated from the Hyperskill template are touched, and the resolver is inserted right after
     * the leading `buildscript { }` block because `plugins { }` may only be preceded by `buildscript { }`
     * and `pluginManagement { }`.
     */
    @VisibleForTesting
    fun addToolchainResolver(content: String): String {
      if (FOOJAY_RESOLVER_ID in content || HS_GRADLE_PLUGIN !in content) return content
      val insertionOffset = buildScriptBlockEndOffset(content) ?: return content
      return content.substring(0, insertionOffset) + TOOLCHAIN_RESOLVER_BLOCK + content.substring(insertionOffset)
    }

    /** Offset right after the closing brace of the leading `buildscript { }` block, or `null` if there is none */
    private fun buildScriptBlockEndOffset(content: String): Int? {
      val blockStart = BUILD_SCRIPT_BLOCK_START.find(content) ?: return null
      var depth = 0
      for (offset in blockStart.range.last..content.lastIndex) {
        when (content[offset]) {
          '{' -> depth++
          '}' -> if (--depth == 0) return offset + 1
        }
      }
      return null
    }
  }
}
