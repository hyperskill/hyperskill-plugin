package org.hyperskill.academy.learning.settings

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurableEP

interface OptionsProvider : Configurable {
  fun isAvailable(): Boolean = true

  companion object {
    val EP_NAME: ExtensionPointName<ConfigurableEP<OptionsProvider>> = ExtensionPointName.create("HyperskillEducational.optionsProvider")
  }
}
