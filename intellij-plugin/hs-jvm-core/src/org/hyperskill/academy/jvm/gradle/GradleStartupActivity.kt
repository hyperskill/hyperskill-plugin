package org.hyperskill.academy.jvm.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils.setupGradleProject
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils.updateGradleSettings
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.StudyTaskManager
import kotlin.coroutines.resume

class GradleStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    if (project.isDisposed || !project.isEduProject()) {
      return
    }
    if (EduGradleUtils.isConfiguredWithGradle(project)) {
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
        continuation.resume(Unit)
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(GradleStartupActivity::class.java)
  }
}
