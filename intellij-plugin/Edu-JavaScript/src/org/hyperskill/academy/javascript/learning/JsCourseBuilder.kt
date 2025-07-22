package org.hyperskill.academy.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeInterpreterUtil
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.lang.javascript.ui.NodeModuleNamesUtil.PACKAGE_JSON
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.DefaultSettingsUtils.findPath
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator

class JsCourseBuilder : EduCourseBuilder<JsNewProjectSettings> {
  override fun taskTemplateName(course: Course): String = JsConfigurator.TASK_JS
  override fun testTemplateName(course: Course): String = JsConfigurator.TEST_JS

  override fun getLanguageSettings(): LanguageSettings<JsNewProjectSettings> = JsLanguageSettings()

  override fun getDefaultSettings(): Result<JsNewProjectSettings, String> {
    return findPath(DEFAULT_NODE_JS_INTERPRETER_PROPERTY, "Node.js interpreter").flatMap { interpreterPath ->
      val interpreter = NodeJsLocalInterpreter(interpreterPath)
      val errorMessage = NodeInterpreterUtil.validateAndGetErrorMessage(interpreter)
      if (errorMessage != null) Err(errorMessage) else Ok(JsNewProjectSettings(interpreter))
    }
  }

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<JsNewProjectSettings> =
    JsCourseProjectGenerator(this, course)

  override fun refreshProject(project: Project, cause: RefreshCause) {
    if (cause == RefreshCause.DEPENDENCIES_UPDATED) {
      val baseDir = project.course?.getDir(project.courseDir) ?: return
      val packageJson = baseDir.findChild(PACKAGE_JSON) ?: return
      installNodeDependencies(project, packageJson)
    }
  }

  companion object {
    private const val DEFAULT_NODE_JS_INTERPRETER_PROPERTY = "project.nodejs.interpreter"
  }
}
