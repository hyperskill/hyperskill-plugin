package org.hyperskill.academy.jvm.gradle

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils.setupGradleProject
import org.hyperskill.academy.jvm.gradle.generation.EduGradleUtils.updateGradleSettings
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.StudyTaskManager

class GradleStartupActivity : StartupActivity.DumbAware {

  override fun runActivity(project: Project) {
    if (project.isDisposed || !project.isEduProject()) {
      return
    }
    if (EduGradleUtils.isConfiguredWithGradle(project)) {
      updateGradleSettings(project)
    }

    DumbService.getInstance(project).runWhenSmart {
      val taskManager = StudyTaskManager.getInstance(project)
      val course = taskManager.course
      if (course == null) {
        LOG.warn("Opened project is with null course")
        return@runWhenSmart
      }

      if (EduGradleUtils.isConfiguredWithGradle(project)) {
        setupGradleProject(project)
      }
    }
  }

  companion object {
    private val LOG = Logger.getInstance(GradleStartupActivity::class.java)
  }
}
