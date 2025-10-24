package org.hyperskill.academy.python.learning.run

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.icons.AllIcons
import org.hyperskill.academy.python.learning.messages.EduPythonBundle.message
import javax.swing.Icon

class PyRunTestsConfigurationType : ConfigurationType {
  override fun getDisplayName(): String = message("tests.study.run")

  override fun getConfigurationTypeDescription(): String = message("tests.study.runner")

  override fun getIcon(): Icon = AllIcons.Actions.Lightning

  // Use a unique ID to avoid collisions with the Edu plugin's configuration type
  override fun getId(): String = "hsPythonRunTests"

  override fun getConfigurationFactories(): Array<ConfigurationFactory> {
    return arrayOf(PyRunTestsConfigurationFactory(this))
  }
}
