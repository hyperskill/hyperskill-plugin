package org.hyperskill.academy.learning

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginManager
import com.intellij.lang.Language
import com.intellij.testFramework.UsefulTestCase
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.EducationalExtensionPoint
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT

// It's intentionally made as an extension property of `UsefulTestCase` to reduce the probability of incorrect usage
// since a plugin descriptor doesn't make sense in some tests
@Suppress("UnusedReceiverParameter")
val UsefulTestCase.testPluginDescriptor: IdeaPluginDescriptor
  get() = PluginManager.getPlugins().first { it.pluginId.idString.startsWith("org.hyperskill.academy") }

inline fun <reified T : EduConfigurator<*>> UsefulTestCase.registerConfigurator(
  language: Language,
  courseType: String = EduFormatNames.PYCHARM,
  environment: String = DEFAULT_ENVIRONMENT
) {
  val extension = EducationalExtensionPoint<EduConfigurator<*>>()
  extension.language = language.id
  extension.implementationClass = T::class.java.name
  extension.courseType = courseType
  extension.environment = environment
  extension.pluginDescriptor = testPluginDescriptor
  EducationalExtensionPoint.EP_NAME.point.registerExtension(extension, testRootDisposable)
}

