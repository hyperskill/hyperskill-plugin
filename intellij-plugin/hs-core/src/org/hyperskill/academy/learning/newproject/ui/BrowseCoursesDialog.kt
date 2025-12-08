package org.hyperskill.academy.learning.newproject.ui

import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.notification.Notification
import com.intellij.notification.Notifications
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.asContextElement
import com.intellij.openapi.updateSettings.impl.UpdateChecker
import com.intellij.openapi.util.Disposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import org.jetbrains.annotations.NonNls
import javax.swing.JComponent
import kotlin.coroutines.CoroutineContext

class BrowseCoursesDialog : OpenCourseDialogBase(), CoroutineScope {
  private val job = SupervisorJob()
  private val panel = CoursesPanelWithTabs(this, disposable)

  override val coroutineContext: CoroutineContext
    get() = job + Dispatchers.Main + ModalityState.any().asContextElement()

  init {
    title = EduCoreBundle.message("course.dialog.title")
    init()
    rootPane.background = SelectCourseBackgroundColor

    Disposer.register(disposable) { job.cancel() }
    setupPluginListeners(disposable)
    panel.loadCourses()
  }

  override fun createCenterPanel(): JComponent = panel

  override fun getPreferredFocusedComponent(): JComponent = panel

  private fun setupPluginListeners(disposable: Disposable) {
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(DynamicPluginListener.TOPIC, object : DynamicPluginListener {
      override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
        panel.doValidation()
      }

      override fun beforePluginUnload(pluginDescriptor: IdeaPluginDescriptor, isUpdate: Boolean) {
        ApplicationManager.getApplication().invokeLater { panel.doValidation() }
      }
    })
    connection
      // TODO: find out a better way to be notified when plugin installation finishes
      .subscribe(Notifications.TOPIC, object : Notifications {
        override fun notify(notification: Notification) {
          if (notification.groupId == UpdateChecker.getNotificationGroup().displayId) {
            panel.doValidation()
            // TODO: investigate why it leads to IDE freeze when you install python plugin
            // ApplicationManager.getApplication().invokeLater {
            //  PluginManagerConfigurable.shutdownOrRestartApp()
            // }
          }
        }
      })
  }

  companion object {
    @NonNls
    const val ACTION_PLACE = "COURSE_SELECTION_DIALOG"
  }
}
