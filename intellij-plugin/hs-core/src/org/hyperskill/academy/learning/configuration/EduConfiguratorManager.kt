package org.hyperskill.academy.learning.configuration

import com.intellij.lang.Language
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PYCHARM

object EduConfiguratorManager {

  /**
   * Returns any enabled [EduConfigurator] for given language, courseType and environment
   */
  fun findConfigurator(courseType: String, environment: String, language: Language): EduConfigurator<*>? =
    findExtension(courseType, environment, language)?.instance

  fun findExtension(courseType: String, environment: String, language: Language): EducationalExtensionPoint<EduConfigurator<*>>? {
    var configurator =
      allExtensions().find { extension ->
        extension.language == language.id &&
        extension.courseType == courseType &&
        extension.environment == environment
      }
    if (configurator == null) {
      configurator = allExtensions().find { extension ->
        extension.language == language.id &&
        compatibleCourseType(extension, courseType) &&
        extension.environment == environment
      }
    }
    return configurator
  }

  /**
   * Returns all extension points of [EduConfigurator] where instance of [EduConfigurator] is enabled
   */
  fun allExtensions(): List<EducationalExtensionPoint<EduConfigurator<*>>> =
    EducationalExtensionPoint.EP_NAME.extensions.filter { it.instance.isEnabled }

  private val compatibleCourseTypes: List<String> = listOf(HYPERSKILL)

  private fun compatibleCourseType(extension: EducationalExtensionPoint<EduConfigurator<*>>, courseType: String): Boolean {
    return extension.courseType == PYCHARM && courseType in compatibleCourseTypes
  }

}
