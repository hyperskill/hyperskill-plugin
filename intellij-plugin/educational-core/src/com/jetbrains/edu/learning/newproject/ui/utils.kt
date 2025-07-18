package com.jetbrains.edu.learning.newproject.ui

import com.intellij.ide.DataManager
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.ColorUtil
import com.intellij.ui.HyperlinkLabel
import com.intellij.ui.JBColor
import com.intellij.util.IconUtil
import com.intellij.util.ui.JBUI
import com.jetbrains.edu.EducationalCoreIcons
import com.jetbrains.edu.learning.EduNames
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.PluginInfo
import com.jetbrains.edu.learning.courseFormat.ext.*
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.HyperskillCourseAdvertiser
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.learning.ui.EduColors
import com.jetbrains.edu.learning.ui.EduColors.getCurrentThemeName
import kotlinx.css.*
import kotlinx.css.properties.lh
import java.awt.Color
import java.awt.Component
import java.awt.FlowLayout
import javax.swing.Icon
import javax.swing.JPanel
import javax.swing.UIManager

private val LOG: Logger = Logger.getInstance("com.jetbrains.edu.learning.newproject.ui.utils")

val Course.logo: Icon?
  get() {
    if (this is HyperskillCourseAdvertiser) {
      return EducationalCoreIcons.Platform.JetBrainsAcademy
    }
    val logo = configurator?.logo ?: compatibilityProvider?.logo
    if (logo == null) {
      val language = languageDisplayName
      LOG.info("configurator and compatibilityProvider are null. language: $language, course type: $itemType, environment: $environment")
    }

    return logo
  }

fun Course.getScaledLogo(logoSize: Int, ancestor: Component): Icon? {
  val logo = logo ?: return null
  val scaleFactor = logoSize / logo.iconHeight.toFloat()
  val scaledIcon = IconUtil.scale(logo, ancestor, scaleFactor)
  return IconUtil.toSize(scaledIcon, JBUI.scale(logoSize), JBUI.scale(logoSize))
}

fun getRequiredPluginsMessage(plugins: Collection<PluginInfo>, actionAsLink: Boolean): String {
  if (plugins.isEmpty()) {
    return ""
  }

  val names = plugins.map { it.displayName ?: it.stringId }
  val message = when (names.size) {
    1 -> EduCoreBundle.message("validation.plugins.required.plugins.one", names[0])
    2 -> EduCoreBundle.message("validation.plugins.required.plugins.two", names[0], names[1])
    3 -> EduCoreBundle.message("validation.plugins.required.plugins.three", names[0], names[1], names[2])
    else -> {
      val restPluginsNumber = plugins.size - 2
      EduCoreBundle.message("validation.plugins.required.plugins.more", names[0], names[1], restPluginsNumber)
    }
  }

  return if (actionAsLink) {
    val link = EduNames.PLUGINS_HELP_LINK
    val action = EduCoreBundle.message("validation.plugins.required.plugins.action")
    "$message <a href='$link'>$action</a>"
  }
  else {
    message
  }
}

fun createCourseDescriptionStylesheet() = CssBuilder().apply {
  body {
    fontFamily = "SF UI Text"
    fontSize = JBUI.scaleFontSize(13.0f).pt
    lineHeight = (JBUI.scaleFontSize(16.0f)).px.lh
  }
}

fun createErrorStylesheet() = CssBuilder().apply {
  a {
    color = EduColors.hyperlinkColor.asCssColor()
  }
}

fun Color.asCssColor(): kotlinx.css.Color = Color("#${ColorUtil.toHex(this)}")


fun createHyperlinkWithContextHelp(actionWrapper: ToolbarActionWrapper): JPanel {
  val action = actionWrapper.action
  val hyperlinkLabel = HyperlinkLabel(actionWrapper.text.get())
  hyperlinkLabel.addHyperlinkListener {
    val actionEvent = AnActionEvent.createFromAnAction(
      action,
      null,
      BrowseCoursesDialog.ACTION_PLACE,
      DataManager.getInstance().getDataContext(hyperlinkLabel)
    )
    action.actionPerformed(actionEvent)
  }

  val hyperlinkPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0)).apply {
    isOpaque = false
  }
  hyperlinkPanel.add(hyperlinkLabel)

  if (action is ContextHelpProvider) {
    hyperlinkPanel.add(action.createContextHelpComponent())
  }

  return hyperlinkPanel
}

fun getColorFromScheme(colorId: String, default: Color): JBColor {
  if (UIManager.getColor(colorId) == null) {
    LOG.warn("Cannot find $colorId for ${getCurrentThemeName()}")
  }
  return JBColor.lazy { UIManager.getColor(colorId) ?: default }
}

fun showNotificationFromCourseValidation(result: CourseValidationResult, title: String) {
  EduNotificationManager
    .create(WARNING, title, result.message)
    .apply {
      when (result) {
        is PluginsRequired -> {
          addAction(object : AnAction(result.actionText()) {
            override fun actionPerformed(e: AnActionEvent) {
              result.showPluginInstallAndEnableDialog()
            }
          })
        }

        is ValidationErrorMessage -> {} // do nothing with the notification
        is ValidationErrorMessageWithHyperlinks ->
          //setting a listener is deprecated, so TextMessageWithHyperlinks should not be used.
          //We need to reword such messages and make viewing a link an action inside a notification
          setListener(NotificationListener.UrlOpeningListener(false))
      }
    }.notify(null)
}