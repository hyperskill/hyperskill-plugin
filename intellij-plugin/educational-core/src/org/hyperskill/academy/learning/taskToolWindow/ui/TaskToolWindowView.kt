package org.hyperskill.academy.learning.taskToolWindow.ui

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsContexts.NotificationContent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.InlineBanner
import com.intellij.ui.InlineBannerBase
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.EduTestAware
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.isHeadlessEnvironment
import org.hyperskill.academy.learning.taskToolWindow.ui.notification.TaskToolWindowNotification.ActionLabel
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TabType
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TaskToolWindowTab
import org.jetbrains.annotations.TestOnly
import java.awt.Color

abstract class TaskToolWindowView(val project: Project) : EduTestAware {

  abstract var currentTask: Task?

  abstract fun init(toolWindow: ToolWindow)

  abstract fun getTab(tabType: TabType): TaskToolWindowTab?
  abstract fun selectTab(tabType: TabType)
  abstract fun isSelectedTab(tabType: TabType): Boolean
  abstract fun updateCheckPanel(task: Task?)
  abstract fun updateTaskSpecificPanel()
  abstract fun updateNavigationPanel(task: Task?)
  abstract fun updateNavigationPanel()
  abstract fun updateTaskDescriptionTab(task: Task?)
  abstract fun updateTaskDescription()
  abstract fun updateTabs(task: Task? = null)
  abstract fun updateTab(tabType: TabType)
  abstract fun showLoadingSubmissionsPanel(platformName: String)
  abstract fun readyToCheck()
  abstract fun scrollNavMap(task: Task?)
  abstract fun checkStarted(task: Task, startSpinner: Boolean = false)
  abstract fun checkFinished(task: Task, checkResult: CheckResult)
  abstract fun addInlineBanner(inlineBanner: InlineBanner)
  abstract fun addInlineBannerToCheckPanel(inlineBanner: InlineBannerBase)

  abstract fun showTaskDescriptionNotification(
    notificationId: String,
    status: EditorNotificationPanel.Status,
    message: @NotificationContent String,
    actionLabel: ActionLabel? = null,
  )

  abstract fun showTaskDescriptionNotificationIfAbsent(
    notificationId: String,
    status: EditorNotificationPanel.Status,
    message: @NotificationContent String,
    actionLabel: ActionLabel? = null
  )

  abstract fun closeExistingTaskDescriptionNotifications(notificationId: String)

  @TestOnly
  override fun cleanUpState() {
    currentTask = null
  }

  companion object {

    fun getInstance(project: Project): TaskToolWindowView {
      if (!project.isEduProject()) {
        error("Attempt to get TaskDescriptionView for non-edu project")
      }
      return project.service()
    }

    fun getTaskDescriptionBackgroundColor(): Color {
      return UIUtil.getListBackground()
    }

    fun updateAllTabs(project: Project) {
      if (isHeadlessEnvironment) return
      getInstance(project).apply {
        updateTaskDescription()
        updateTabs()
      }
    }
  }
}
