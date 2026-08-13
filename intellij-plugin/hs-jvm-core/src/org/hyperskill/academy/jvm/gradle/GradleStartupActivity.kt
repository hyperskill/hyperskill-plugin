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
      val buildScriptMigrated = migrateLegacyBuildGradle(project)
      updateGradleSettings(project)
      if (buildScriptMigrated) {
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
   * Rewrites `project(':util').sourceSets.*.output` references left in build scripts of old projects.
   *
   * Inside a `dependencies { }` block such a call is resolved against `DependencyHandler`,
   * which since Gradle 9 provides its own `project(String)` returning a `ProjectDependency` instead of a `Project`,
   * so `sourceSets` is no longer resolvable there. Qualifying the call with `rootProject` makes it resolve
   * against `Project` again, which is valid for every Gradle version, so the migration is not tied to a
   * particular IDE or Gradle version.
   *
   * @return `true` if the build script was actually changed
   */
  private suspend fun migrateLegacyBuildGradle(project: Project): Boolean {
    val projectDir = project.guessProjectDir() ?: return false
    val buildFile = projectDir.findChild(GradleConstants.DEFAULT_SCRIPT_NAME) ?: return false
    if (buildFile.isDirectory) return false

    return try {
      val originalContent = VfsUtilCore.loadText(buildFile)
      val migratedContent = migrateLegacyUtilSourceSetReferences(originalContent)
      if (migratedContent == originalContent) return false

      writeAction {
        VfsUtil.saveText(buildFile, migratedContent)
      }
      LOG.info("Migrated legacy util sourceSets references in ${buildFile.path}")
      true
    }
    catch (e: IOException) {
      LOG.warn("Failed to migrate legacy util sourceSets references in ${buildFile.path}", e)
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

    @VisibleForTesting
    fun migrateLegacyUtilSourceSetReferences(content: String): String =
      content.replace(LEGACY_UTIL_SOURCE_SET_REFERENCE) { matchResult -> "rootProject.${matchResult.value}" }
  }
}
