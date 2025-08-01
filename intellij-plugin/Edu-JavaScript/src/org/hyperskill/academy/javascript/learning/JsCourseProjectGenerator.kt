package org.hyperskill.academy.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.library.core.NodeCoreLibraryConfigurator
import com.intellij.javascript.nodejs.settings.NodeSettingsConfigurable
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator

class JsCourseProjectGenerator(builder: JsCourseBuilder, course: Course) : CourseProjectGenerator<JsNewProjectSettings>(builder, course) {
  override fun afterProjectGenerated(
    project: Project,
    projectSettings: JsNewProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    val interpreter = projectSettings.selectedInterpreter
    if (interpreter == null) {
      // It's ok not to have NodeJS interpreter in tests
      if (!isUnitTestMode) {
        LOG.warn("NodeJS interpreter is not selected")
      }
      return
    }
    NodeJsInterpreterManager.getInstance(project).setInterpreterRef(interpreter.toRef())

    val packageJsonFile = project.courseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
    // Don't install dependencies in headless mode (tests or course creation using `EduCourseCreatorAppStarter` on remote)
    // It doesn't make sense for tests since we don't check it.
    // On remote dependencies will be installed during warmup phase
    if (packageJsonFile != null && !isHeadlessEnvironment) {
      installNodeDependencies(project, packageJsonFile)
    }

    configureNodeJS(interpreter, project, onConfigurationFinished)
    // Pass empty callback here because Core library configuration will be made asynchronously
    // Before this, we can't consider JS course project is fully configured
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished = {})
  }

  private fun configureNodeJS(
    interpreter: NodeJsInterpreter,
    project: Project,
    onConfigurationFinished: () -> Unit
  ) {
    if (isUnitTestMode) return

    val modalityState = ModalityState.current()
    interpreter.provideCachedVersionOrFetch { version ->
      project.invokeLater(modalityState) {
        if (version != null) {
          val configurator = NodeCoreLibraryConfigurator.getInstance(project)
          configurator.configureAndAssociateWithProject(interpreter, version) {
            onConfigurationFinished()
          }
        }
        else {
          LOG.warn("Couldn't retrieve Node interpreter version")
          @Suppress("UnstableApiUsage")
          val requester = ModuleManager.getInstance(project).modules[0].moduleFile
          NodeSettingsConfigurable.showSettingsDialog(project, requester)
          onConfigurationFinished()
        }
      }
    }
  }

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    val packageJsonFile = holder.courseDir.findChild(NodeModuleNamesUtil.PACKAGE_JSON)
    if (packageJsonFile == null && !holder.course.isStudy) {
      val templateText = getInternalTemplateText(NodeModuleNamesUtil.PACKAGE_JSON)
      return listOf(EduFile(NodeModuleNamesUtil.PACKAGE_JSON, templateText))
    }
    return emptyList()
  }

  companion object {
    private val LOG = logger<JsCourseProjectGenerator>()
  }
}