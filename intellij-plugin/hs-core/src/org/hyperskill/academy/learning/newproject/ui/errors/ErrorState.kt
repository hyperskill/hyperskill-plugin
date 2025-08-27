package org.hyperskill.academy.learning.newproject.ui.errors

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.compatibility.CourseCompatibility
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import org.hyperskill.academy.learning.courseFormat.PluginInfo
import org.hyperskill.academy.learning.courseFormat.ext.compatibility
import org.hyperskill.academy.learning.courseFormat.ext.languageDisplayName
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.HyperskillCourseAdvertiser
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.newproject.ui.errors.ErrorSeverity.*
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessageType.ERROR
import org.hyperskill.academy.learning.newproject.ui.errors.ValidationMessageType.WARNING
import org.hyperskill.academy.learning.newproject.ui.getRequiredPluginsMessage
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.jetbrains.annotations.Nls

sealed class ErrorState(
  private val severity: ErrorSeverity,
  val message: ValidationMessage?,
  val courseCanBeStarted: Boolean
) {

  object NothingSelected : ErrorState(OK, null, false)
  object None : ErrorState(OK, null, true)
  object Pending : ErrorState(LANGUAGE_SETTINGS_PENDING, null, false)

  object NotLoggedIn : ErrorState(
    LOGIN_RECOMMENDED,
    ValidationMessage(EduCoreBundle.message("validation.stepik.log.in.needed"), type = WARNING),
    true
  )

  object HyperskillLoginNeeded : ErrorState(
    LOGIN_ERROR,
    ValidationMessage(EduCoreBundle.message("validation.hyperskill.login.needed")),
    false
  )

  abstract class LocationError(messageText: String) : ErrorState(
    LOCATION_ERROR,
    ValidationMessage(messageText, type = ERROR),
    false
  )

  object EmptyLocation : LocationError(EduCoreBundle.message("validation.empty.location"))
  object InvalidLocation : LocationError(EduCoreBundle.message("validation.cannot.create.course.at.location"))

  abstract class LoginRequired(
    platformName: String
  ) : ErrorState(
    LOGIN_ERROR,
    ValidationMessage(EduCoreBundle.message("validation.log.in.to.start.course", platformName)),
    false
  )

  //TODO: remove it?
  object HyperskillLoginRequired : LoginRequired(EduNames.JBA)
  class CustomSevereError(message: String, val action: Runnable? = null) :
    ErrorState(LOGIN_ERROR, ValidationMessage(message), false)

  class LanguageSettingsError(message: ValidationMessage) : ErrorState(LANGUAGE_SETTINGS_ERROR, message, false)

  object JCEFRequired : ErrorState(
    NO_JCEF, ValidationMessage(
      EduCoreBundle.message("validation.no.jcef")
    ), false
  )

  object IncompatibleVersion : ErrorState(
    PLUGIN_UPDATE_REQUIRED, ValidationMessage(EduCoreBundle.message("validation.plugins.required")),
    false
  )

  data class RequirePlugins(val pluginIds: List<PluginInfo>) : ErrorState(
    PLUGIN_UPDATE_REQUIRED, errorMessageForRequiredPlugins(pluginIds),
    false
  )

  object RestartNeeded : ErrorState(
    PLUGIN_UPDATE_REQUIRED,
    ValidationMessage(EduCoreBundle.message("validation.plugins.restart.to.activate.plugin")),
    false
  )

  class UnsupportedCourse(@Nls(capitalization = Nls.Capitalization.Sentence) message: String) : ErrorState(
    UNSUPPORTED_COURSE,
    ValidationMessage(message),
    false
  )

  fun merge(other: ErrorState): ErrorState = if (severity < other.severity) other else this

  companion object {
    fun forCourse(course: Course?): ErrorState {
      if (course == null) return NothingSelected
      return None
        .merge(course.compatibilityError)
        .merge(course.courseSpecificError)
    }

    private val Course.compatibilityError: ErrorState
      get() {
        return when (val compatibility = compatibility) {
          CourseCompatibility.Unsupported -> UnsupportedCourse(unsupportedCourseMessage)
          CourseCompatibility.IncompatibleVersion -> IncompatibleVersion
          is CourseCompatibility.PluginsRequired -> {
            if (compatibility.toInstallOrEnable.isEmpty()) RestartNeeded else RequirePlugins(compatibility.toInstallOrEnable)
          }

          else -> None
        }
      }

    private val Course.unsupportedCourseMessage: String
      @Nls(capitalization = Nls.Capitalization.Sentence)
      get() {
        val type = when (val environment = course.environment) {
          EduNames.ANDROID -> environment
          DEFAULT_ENVIRONMENT -> course.languageDisplayName
          else -> null
        }
        return if (type != null) {
          EduCoreBundle.message("courses.not.supported", type)
        }
        else {
          EduCoreBundle.message("selected.course.not.supported", course.name)
        }
      }

    private val Course.courseSpecificError: ErrorState
      get() {
        return when {
          CoursesStorage.getInstance().hasCourse(this) -> None
          this is HyperskillCourseAdvertiser -> if (HyperskillSettings.INSTANCE.account == null) HyperskillLoginNeeded else None
          this is HyperskillCourse -> if (HyperskillSettings.INSTANCE.account == null) HyperskillLoginRequired else None
          else -> None
        }
      }

    private fun errorMessageForRequiredPlugins(plugins: Collection<PluginInfo>): ValidationMessage {
      return ValidationMessage(getRequiredPluginsMessage(plugins, actionAsLink = true))
    }

    fun errorMessage(disabledPluginIds: Collection<PluginId>): ValidationMessage {
      val pluginName = if (disabledPluginIds.size == 1) {
        PluginManagerCore.getPlugin(disabledPluginIds.first())?.name
      }
      else {
        null
      }
      return if (pluginName != null) {
        ValidationMessage(EduCoreBundle.message("validation.plugins.disabled.plugin", pluginName))
      }
      else {
        ValidationMessage(EduCoreBundle.message("validation.plugins.disabled.or.not.installed.plugins"))
      }
    }

  }
}

/**
 * Order is meaningful. Errors with more priority are located lower
 */
private enum class ErrorSeverity {
  OK,

  LANGUAGE_SETTINGS_PENDING,

  LOGIN_RECOMMENDED,
  LOGIN_ERROR,

  LANGUAGE_SETTINGS_ERROR,
  LOCATION_ERROR,

  NO_JCEF,

  PLUGIN_UPDATE_REQUIRED,

  UNSUPPORTED_COURSE
}

