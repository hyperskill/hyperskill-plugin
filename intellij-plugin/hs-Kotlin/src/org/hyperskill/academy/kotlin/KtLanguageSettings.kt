package org.hyperskill.academy.kotlin

import org.hyperskill.academy.jvm.JdkLanguageSettings
import org.hyperskill.academy.kotlin.messages.EduKotlinBundle
import org.hyperskill.academy.learning.DEFAULT_KOTLIN_VERSION
import org.hyperskill.academy.learning.EduNames.ENVIRONMENT_CONFIGURATION_LINK_KOTLIN
import org.hyperskill.academy.learning.KotlinVersion
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.kotlinVersion
import org.hyperskill.academy.learning.newproject.ui.errors.SettingsValidationResult
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessage

class KtLanguageSettings : JdkLanguageSettings() {

  override fun validate(course: Course?, courseLocation: String?): SettingsValidationResult {
    if (course != null) {
      val courseKotlinVersion = course.kotlinVersion
      val kotlinVersion = kotlinVersion()
      if (kotlinVersion < courseKotlinVersion) {
        val validationMessage = ValidationMessage(
          EduKotlinBundle.message("update.kotlin.plugin", courseKotlinVersion.version),
          ENVIRONMENT_CONFIGURATION_LINK_KOTLIN
        )
        return SettingsValidationResult.Ready(validationMessage)
      }
    }
    return super.validate(course, courseLocation)
  }

  private val Course.kotlinVersion: KotlinVersion
    get() {
      val langVersion = course.languageVersion ?: return DEFAULT_KOTLIN_VERSION
      return KotlinVersion(langVersion)
    }
}