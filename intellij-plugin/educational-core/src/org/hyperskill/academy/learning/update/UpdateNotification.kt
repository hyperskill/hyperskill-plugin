package org.hyperskill.academy.learning.update

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.WARNING
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.installAndEnablePlugin
import org.hyperskill.academy.learning.notification.EduNotificationManager

fun showUpdateNotification(
  project: Project,
  @NlsContexts.NotificationTitle title: String,
  @NlsContexts.NotificationContent content: String
) {
  EduNotificationManager
    .create(WARNING, title, content)
    .apply {
      addAction(NotificationAction.createSimple("Update") {
        installAndEnablePlugin(setOf(PluginId.getId(EduNames.PLUGIN_ID))) {
          this@apply.expire()
        }
      })
    }
    .notify(project)
}
