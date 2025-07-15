package com.jetbrains.edu.learning.marketplace

import com.intellij.icons.AllIcons
import com.intellij.notification.BrowseNotificationAction
import com.intellij.notification.NotificationType.ERROR
import com.intellij.notification.NotificationType.INFORMATION
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.EduFormatNames.FAILED_TO_DELETE_SUBMISSIONS
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.notification.EduNotificationManager
import org.jetbrains.annotations.NotNull

object MarketplaceNotificationUtils {
  fun showLoginNeededNotification(
    project: Project?,
    failedActionTitle: String,
    notificationTitle: String = EduCoreBundle.message("notification.title.authorization.required"),
    authAction: () -> Unit
  ) {
    EduNotificationManager
      .create(ERROR, notificationTitle, EduCoreBundle.message("notification.content.authorization", failedActionTitle))
      .apply {
        @Suppress("DialogTitleCapitalization")
        addAction(object : DumbAwareAction(EduCoreBundle.message("notification.content.authorization.action")) {
          override fun actionPerformed(e: AnActionEvent) {
            authAction()
            this@apply.expire()
          }
        })
      }.notify(project)
  }

  internal fun showSubmissionsDeletedSuccessfullyNotification(project: Project?, courseId: Int?, loginName: String?) {
    val presentableName = project?.course?.presentableName
    val message = if (loginName != null && courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.for.user.on.course.success.message", loginName, presentableName)
    }
    else if (loginName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.for.user.success.message", loginName)
    }
    else if (courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.success.on.course.message", presentableName)
    }
    else {
      EduCoreBundle.message("marketplace.delete.submissions.success.message")
    }

    EduNotificationManager.showInfoNotification(
      project,
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("marketplace.delete.submissions.success.title"),
      message
    )
  }

  internal fun showNoSubmissionsToDeleteNotification(project: Project?, courseId: Int?, loginName: String?) {
    val presentableName = project?.course?.presentableName
    val message = if (loginName != null && courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.for.user.on.course.nothing.message", loginName, presentableName)
    }
    else if (loginName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.for.user.nothing.message", loginName)
    }
    else if (courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.on.course.nothing.message", presentableName)
    }
    else {
      EduCoreBundle.message("marketplace.delete.submissions.nothing.message")
    }

    EduNotificationManager.showInfoNotification(
      project,
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("marketplace.delete.submissions.nothing.title"),
      message
    )
  }


  internal fun showFailedToDeleteNotification(project: Project?, courseId: Int?, loginName: String?) {
    val presentableName = project?.course?.presentableName
    val message = if (loginName != null && courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.failed.for.user.on.course.message", loginName, presentableName)
    }
    else if (loginName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.failed.for.user.message", loginName)
    }
    else if (courseId != null && presentableName != null) {
      EduCoreBundle.message("marketplace.delete.submissions.failed.on.course.message", presentableName)
    }
    else {
      EduCoreBundle.message("marketplace.delete.submissions.failed.message")
    }

    EduNotificationManager.create(
      ERROR,
      @Suppress("DialogTitleCapitalization") EduCoreBundle.message("marketplace.delete.submissions.failed.title"),
      message
    ).addAction(
      BrowseNotificationAction(
        EduCoreBundle.message("marketplace.delete.submissions.failed.troubleshooting.text"), FAILED_TO_DELETE_SUBMISSIONS
      )
    ).notify(project)
  }

  fun showSuccessRequestNotification(
    project: Project?,
    @NotNull @NlsContexts.NotificationTitle title: String,
    @NotNull @NlsContexts.NotificationContent message: String
  ) {
    EduNotificationManager
      .create(INFORMATION, title, message)
      // workaround: there is no NotificationType.Success in the platform yet
      .setIcon(AllIcons.Status.Success)
      .notify(project)
  }
}