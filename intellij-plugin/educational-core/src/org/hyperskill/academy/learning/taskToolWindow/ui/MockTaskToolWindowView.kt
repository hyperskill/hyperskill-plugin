package org.hyperskill.academy.learning.taskToolWindow.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.InlineBannerBase
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.taskToolWindow.ui.notification.TaskToolWindowNotification.ActionLabel
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TabType
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TaskToolWindowTab

class MockTaskToolWindowView(project: Project) : TaskToolWindowView(project) {
  override var currentTask: Task? = null

  override fun init(toolWindow: ToolWindow) {}
  override fun getTab(tabType: TabType): TaskToolWindowTab? = null
  override fun selectTab(tabType: TabType) {}
  override fun isSelectedTab(tabType: TabType): Boolean = false
  override fun updateCheckPanel(task: Task?) {}
  override fun updateTaskSpecificPanel() {}
  override fun updateNavigationPanel(task: Task?) {}
  override fun updateNavigationPanel() {}
  override fun updateTaskDescriptionTab(task: Task?) {}
  override fun updateTaskDescription() {}
  override fun updateTabs(task: Task?) {}
  override fun updateTab(tabType: TabType) {}
  override fun showLoadingSubmissionsPanel(platformName: String) {}
  override fun readyToCheck() {}
  override fun scrollNavMap(task: Task?) {}
  override fun checkStarted(task: Task, startSpinner: Boolean) {}
  override fun checkFinished(task: Task, checkResult: CheckResult) {}
  override fun addInlineBanner(inlineBanner: InlineBanner) {}
  override fun addInlineBannerToCheckPanel(inlineBanner: InlineBannerBase) {}
  override fun showTaskDescriptionNotification(
    notificationId: String,
    status: EditorNotificationPanel.Status,
    message: @NotificationContent String,
    actionLabel: ActionLabel?
  ) {
  }

  override fun showTaskDescriptionNotificationIfAbsent(
    notificationId: String,
    status: EditorNotificationPanel.Status,
    message: @NotificationContent String,
    actionLabel: ActionLabel?
  ) {
  }

  override fun closeExistingTaskDescriptionNotifications(notificationId: String) {}
}
