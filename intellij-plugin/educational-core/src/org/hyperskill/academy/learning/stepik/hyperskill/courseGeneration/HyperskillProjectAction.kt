package org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration

import com.intellij.notification.Notification
import com.intellij.notification.NotificationListener
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.ui.MessageType
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.components.AnActionLink
import org.hyperskill.academy.learning.EduBrowser
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.RemoteEnvHelper
import org.hyperskill.academy.learning.authUtils.AuthorizationPlace
import org.hyperskill.academy.learning.courseFormat.ext.CourseValidationResult
import org.hyperskill.academy.learning.courseFormat.ext.PluginsRequired
import org.hyperskill.academy.learning.courseFormat.ext.ValidationErrorMessage
import org.hyperskill.academy.learning.courseFormat.ext.ValidationErrorMessageWithHyperlinks
import org.hyperskill.academy.learning.courseGeneration.ProjectOpener
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.stepik.hyperskill.SELECT_PROJECT
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.getSelectedProjectIdUnderProgress
import org.hyperskill.academy.learning.stepik.hyperskill.isHyperskillSupportAvailable
import java.net.URL
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class HyperskillProjectAction : DumbAwareAction(EduCoreBundle.message("hyperskill.open.project.text")) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isHyperskillSupportAvailable() && !RemoteEnvHelper.isRemoteDevServer()
  }

  override fun actionPerformed(e: AnActionEvent) {
    if (HyperskillConnector.getInstance().getCurrentUserInfo() == null) {
      showBalloon(e, "Please <a href=\"\">log in to ${EduNames.JBA}</a> and select a project.", HSHyperlinkListener(true))
      return
    }

    openHyperskillProject { error ->

      val hyperlinkListener = when (error) {
        is PluginsRequired -> object : HyperlinkAdapter() {
          override fun hyperlinkActivated(e: HyperlinkEvent) {
            error.showPluginInstallAndEnableDialog()
          }
        }

        is ValidationErrorMessage -> null
        is ValidationErrorMessageWithHyperlinks -> HSHyperlinkListener(false)
      }

      val message = when (error) {
        is PluginsRequired -> "${error.message} <a href=''>${error.actionText()}</a>"
        else -> error.message
      }

      showBalloon(e, message, hyperlinkListener)
    }
  }

  private fun showBalloon(e: AnActionEvent, message: String, hyperlinkListener: HyperlinkListener?) {
    val builder = JBPopupFactory.getInstance().createHtmlTextBalloonBuilder(message, MessageType.INFO, hyperlinkListener)
    builder.setHideOnClickOutside(true)
    val balloon = builder.createBalloon()

    val component = e.getData(PlatformDataKeys.CONTEXT_COMPONENT)
    if (component is AnActionLink) {
      balloon.showInCenterOf(component)
    }
    else {
      val relativePoint = JBPopupFactory.getInstance().guessBestPopupLocation(e.dataContext)
      balloon.show(relativePoint, Balloon.Position.above)
    }
  }

  companion object {
    fun openHyperskillProject(showError: (CourseValidationResult) -> Unit): Boolean {
      val projectId = getSelectedProjectIdUnderProgress()
      if (projectId == null) {
        showError(ValidationErrorMessageWithHyperlinks(SELECT_PROJECT))
        return false
      }

      return ProjectOpener.getInstance()
        .open(HyperskillOpenInIdeRequestHandler, HyperskillOpenProjectStageRequest(projectId, null)).onError {
          showError(it)
          return false
        }
    }
  }
}

class HSHyperlinkListener(private val authorize: Boolean) : NotificationListener, HyperlinkAdapter() {
  override fun hyperlinkUpdate(notification: Notification, event: HyperlinkEvent) {
    authorizeOrBrowse(event.url)
  }

  private fun authorizeOrBrowse(url: URL?) {
    if (authorize) {
      HyperskillConnector.getInstance().doAuthorize(authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG)
    }
    else {
      if (url != null) {
        EduBrowser.getInstance().browse(url)
      }
    }
  }

  override fun hyperlinkActivated(e: HyperlinkEvent) {
    authorizeOrBrowse(e.url)
  }
}
