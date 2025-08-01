package org.hyperskill.academy.learning.newproject.ui.errors

import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.ide.plugins.PluginStateListener
import com.intellij.ide.plugins.PluginStateManager
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.util.Disposer
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.actions.SwitchTaskPanelAction
import org.hyperskill.academy.learning.authUtils.AuthorizationPlace
import org.hyperskill.academy.learning.installAndEnablePlugin
import org.hyperskill.academy.learning.newproject.ui.CoursesPanel
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CoursePanel
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import javax.swing.JTextPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener

class ErrorStateHyperlinkListener(private val parentDisposable: Disposable) : HyperlinkListener {
  override fun hyperlinkUpdate(e: HyperlinkEvent?) {
    if (e?.eventType != HyperlinkEvent.EventType.ACTIVATED) return

    val coursePanel = UIUtil.getParentOfType(CoursePanel::class.java, e.source as? JTextPane) ?: return
    val coursesPanel = UIUtil.getParentOfType(CoursesPanel::class.java, e.source as? JTextPane)
    val postLoginActions = arrayOf(
      Runnable { coursePanel.hideErrorPanel() },
      Runnable { doValidation(coursePanel) },
      Runnable { coursesPanel?.scheduleUpdateAfterLogin() }
    )

    when (val state = coursePanel.errorState) {
      is ErrorState.HyperskillLoginNeeded -> {
        HyperskillConnector.getInstance().doAuthorize(*postLoginActions, authorizationPlace = AuthorizationPlace.START_COURSE_DIALOG)
      }

      is ErrorState.NotLoggedIn -> {
        // TODO: Update course list
      }

      is ErrorState.JCEFRequired -> invokeSwitchUILibrary(coursePanel)
      is ErrorState.IncompatibleVersion -> installAndEnablePlugin(setOf(PluginId.getId(EduNames.PLUGIN_ID))) {}
      is ErrorState.RequirePlugins -> {
        val listener = object : PluginStateListener {
          override fun install(descriptor: IdeaPluginDescriptor) {
            coursePanel.doValidation()
          }

          override fun uninstall(descriptor: IdeaPluginDescriptor) {

          }
        }

        Disposer.register(parentDisposable) {
          PluginStateManager.removeStateListener(listener)
        }
        val pluginStringIds = state.pluginIds.mapTo(HashSet()) { PluginId.getId(it.stringId) }
        PluginStateManager.addStateListener(listener)
        installAndEnablePlugin(pluginStringIds) {}
      }

      is ErrorState.RestartNeeded -> {
        //close Course Selection View if it's open
        DialogWrapper.findInstance(coursesPanel)?.close(DialogWrapper.OK_EXIT_CODE)

        //close individual Start Course dialog if it's open
        DialogWrapper.findInstance(coursePanel)?.close(DialogWrapper.OK_EXIT_CODE)

        ApplicationManager.getApplication().exit(true, true, true)
      }

      is ErrorState.CustomSevereError -> state.action?.run()
      else -> browseHyperlink(state.message)
    }
  }

  private fun invokeSwitchUILibrary(coursePanel: CoursePanel) {
    val switchUILibraryAction = SwitchTaskPanelAction.ACTION_ID
    val action = ActionManager.getInstance().getAction(switchUILibraryAction)
    if (action == null) {
      Logger.getInstance(CoursesPanel::class.java).error("$switchUILibraryAction action not found")
      return
    }
    ActionManager.getInstance().tryToExecute(
      action,
      null,
      coursePanel,
      ActionPlaces.UNKNOWN,
      true
    )
    doValidation(coursePanel)
  }

  private fun doValidation(coursePanel: CoursePanel) {
    val course = coursePanel.course ?: return
    val errorState = getErrorState(course) { coursePanel.validateSettings(course) }
    coursePanel.setError(errorState)
    coursePanel.setButtonsEnabled(errorState.courseCanBeStarted)
  }

}
