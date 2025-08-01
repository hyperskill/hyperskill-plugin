package org.hyperskill.academy.javascript.learning

import com.intellij.javascript.nodejs.interpreter.NodeInterpreterUtil
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreter
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.util.CheckedDisposable
import com.intellij.openapi.util.UserDataHolder
import org.hyperskill.academy.javascript.learning.messages.EduJavaScriptBundle
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessage
import java.awt.BorderLayout
import javax.swing.JComponent

class JsLanguageSettings : LanguageSettings<JsNewProjectSettings>() {
  private val jsSettings = JsNewProjectSettings()
  private val interpreterField: NodeJsInterpreterField

  init {
    val defaultProject = ProjectManager.getInstance().defaultProject
    interpreterField = object : NodeJsInterpreterField(defaultProject, false) {
      override fun isDefaultProjectInterpreterField(): Boolean {
        return true
      }
    }
    interpreterField.addChangeListener { interpreter: NodeJsInterpreter? ->
      jsSettings.selectedInterpreter = interpreter
      notifyListeners()
    }
    interpreterField.interpreterRef = NodeJsInterpreterManager.getInstance(defaultProject).interpreterRef
  }

  override fun getLanguageSettingsComponents(
    course: Course,
    disposable: CheckedDisposable,
    context: UserDataHolder?
  ): List<LabeledComponent<JComponent>> {
    return listOf(LabeledComponent.create(interpreterField, EduCoreBundle.message("select.interpreter"), BorderLayout.WEST))
  }

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    val interpreter = interpreterField.interpreter
    val message = NodeInterpreterUtil.validateAndGetErrorMessage(interpreter) ?: return SettingsValidationResult.OK
    val validationMessage = ValidationMessage(
      EduJavaScriptBundle.message("configure.js.environment.help", message, EduNames.ENVIRONMENT_CONFIGURATION_LINK_JS),
      EduNames.ENVIRONMENT_CONFIGURATION_LINK_JS
    )
    return SettingsValidationResult.Ready(validationMessage)
  }

  override fun getSettings(): JsNewProjectSettings = jsSettings
}
