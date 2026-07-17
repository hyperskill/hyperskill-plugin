package org.hyperskill.academy.jvm.gradle

import com.intellij.openapi.application.ApplicationInfo
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
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.IOException
import kotlin.coroutines.resume

class GradleStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    if (project.isDisposed || !project.isEduProject()) {
      return
    }
    if (EduGradleUtils.isConfiguredWithGradle(project)) {
      migrateLegacyBuildGradle(project)
      updateGradleSettings(project)
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

  private suspend fun migrateLegacyBuildGradle(project: Project) {
    if (ApplicationInfo.getInstance().build.baselineVersion != IDEA_2026_2_BASELINE) return

    val projectDir = project.guessProjectDir() ?: return
    val buildFile = projectDir.findChild(GradleConstants.DEFAULT_SCRIPT_NAME) ?: return
    if (buildFile.isDirectory) return

    try {
      val originalContent = VfsUtilCore.loadText(buildFile)
      val migratedContent = originalContent.replace(LEGACY_UTIL_SOURCE_SET_REFERENCE) { matchResult ->
        "rootProject.${matchResult.value}"
      }
      if (migratedContent == originalContent) return

      writeAction {
        VfsUtil.saveText(buildFile, migratedContent)
      }
      LOG.info("Migrated legacy util sourceSets references in ${buildFile.path}")
    }
    catch (e: IOException) {
      LOG.warn("Failed to migrate legacy util sourceSets references in ${buildFile.path}", e)
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

    private const val IDEA_2026_2_BASELINE = 262
    private const val UTIL_MODULE_NAME = "util"

    private val LEGACY_UTIL_SOURCE_SET_REFERENCE =
      Regex("""(?<![\w.])project\(':util'\)\.sourceSets\.(?:main|test)\.output""")
  }
}
