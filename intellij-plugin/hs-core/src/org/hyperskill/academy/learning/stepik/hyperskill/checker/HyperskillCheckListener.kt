package org.hyperskill.academy.learning.stepik.hyperskill.checker

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.checker.CheckListener
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillLoginListener
import org.hyperskill.academy.learning.stepik.hyperskill.metrics.HyperskillMetricsService
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import javax.swing.event.HyperlinkEvent

class HyperskillCheckListener : CheckListener {

  override fun beforeCheck(project: Project, task: Task) {
    if (task.lesson.course !is HyperskillCourse) {
      return
    }

    HyperskillMetricsService.getInstance().taskStopped()
  }

  override fun afterCheck(project: Project, task: Task, result: CheckResult) {
    if (task.lesson.course !is HyperskillCourse) {
      return
    }
    // this method is called from EDT, so it means that between if check and if body
    // user cannot navigate between tasks
    require(ApplicationManager.getApplication().isDispatchThread)
    if (result != CheckResult.SOLVED && project.getCurrentTask()?.id == task.id) {
      HyperskillMetricsService.getInstance().taskStarted(task)
    }
    sendSolution(task, project, result)
  }

  private fun sendSolution(
    task: Task,
    project: Project,
    result: CheckResult
  ) {
    if (HyperskillCheckConnector.isRemotelyChecked(task) || task is TheoryTask) {
      /**
       * Solution must be sent after local tests check are made for Edu tasks.
       * Opposite to Edu tasks, e.g., there are no local tests check for Code tasks and code is submitted directly to JBA.
       */
      return
    }

    val course = task.lesson.course as? HyperskillCourse ?: return

    if (HyperskillSettings.INSTANCE.account == null) {
      EduNotificationManager.create(
        ERROR,
        EduCoreBundle.message("error.failed.to.post.solution.to", EduNames.JBA),
        EduCoreBundle.message("error.login.required", EduNames.JBA),
      ).apply {
        addAction(NotificationAction.createSimple("Login") {
          this@apply.expire()
          HyperskillLoginListener.hyperlinkUpdate(HyperlinkEvent(this, HyperlinkEvent.EventType.ACTIVATED, null, null))
        })
      }.notify(project)
      return
    }

    if (!isUnitTestMode) {
      ApplicationManager.getApplication().executeOnPooledThread {
        HyperskillCheckConnector.postEduTaskSolution(task, project, result)
      }
    }
    else {
      HyperskillCheckConnector.postEduTaskSolution(task, project, result)
    }
  }
}