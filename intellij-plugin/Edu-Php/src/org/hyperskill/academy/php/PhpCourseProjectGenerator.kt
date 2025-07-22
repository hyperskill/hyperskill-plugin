package org.hyperskill.academy.php

import com.intellij.openapi.project.Project
import com.jetbrains.php.composer.ComposerDataService
import com.jetbrains.php.composer.ComposerUtils
import com.jetbrains.php.composer.actions.ComposerInstallAction
import com.jetbrains.php.composer.actions.ComposerOptionsManager
import com.jetbrains.php.composer.execution.phar.PharComposerExecution
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import org.hyperskill.academy.learning.invokeLater
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator

class PhpCourseProjectGenerator(
  builder: PhpCourseBuilder,
  course: Course
) : CourseProjectGenerator<PhpProjectSettings>(builder, course) {

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> =
    listOfNotNull(createComposerFile(holder))

  override fun afterProjectGenerated(
    project: Project,
    projectSettings: PhpProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    if (!isUnitTestMode) {
      downloadPhar(project, projectSettings)
      installComposer(project)
    }
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }

  private fun createComposerFile(holder: CourseInfoHolder<Course>): EduFile? {
    val composerFile = holder.courseDir.findChild(ComposerUtils.CONFIG_DEFAULT_FILENAME)
    if (composerFile != null) return null

    return EduFile(ComposerUtils.CONFIG_DEFAULT_FILENAME, getInternalTemplateText(ComposerUtils.CONFIG_DEFAULT_FILENAME))
  }

  private fun downloadPhar(project: Project, projectSettings: PhpProjectSettings) {
    val interpreterId = projectSettings.phpInterpreter?.id
    val file = ComposerUtils.downloadPhar(project, null, project.basePath)
    val pharComposerExecution = if (file != null) {
      PharComposerExecution(interpreterId, file.path, false)
    }
    else {
      PharComposerExecution(interpreterId, null, true)
    }
    ComposerDataService.getInstance(project).composerExecution = pharComposerExecution
  }

  private fun installComposer(project: Project) {
    val courseDir = project.courseDir
    val composerFile = courseDir.findChild(ComposerUtils.CONFIG_DEFAULT_FILENAME) ?: return
    project.invokeLater {
      val executor = ComposerInstallAction.createExecutor(
        project,
        ComposerDataService.getInstance(project).composerExecution,
        composerFile,
        ComposerOptionsManager.DEFAULT_COMMAND_LINE_OPTIONS,
        null,
        true
      )
      executor.execute()
    }
  }
}
