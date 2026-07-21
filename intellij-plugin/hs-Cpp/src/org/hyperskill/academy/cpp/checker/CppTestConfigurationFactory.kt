package org.hyperskill.academy.cpp.checker

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationTypeUtil

internal const val GOOGLE_TEST_CONFIGURATION_TYPE_ID = "CMakeGoogleTestRunConfigurationType"
internal const val CATCH_TEST_CONFIGURATION_TYPE_ID = "CMakeCatchTestRunConfigurationType"

internal fun getTestConfigurationFactory(configurationTypeId: String): ConfigurationFactory =
  ConfigurationTypeUtil.findConfigurationType(configurationTypeId)
    ?.configurationFactories
    ?.singleOrNull()
  ?: error("Cannot find a single run configuration factory for '$configurationTypeId'")
