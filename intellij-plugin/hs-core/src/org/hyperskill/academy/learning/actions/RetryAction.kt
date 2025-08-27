package org.hyperskill.academy.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions.ActionText
import org.hyperskill.academy.learning.EduUtilsKt.showPopup
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.checker.remote.RemoteTaskCheckerManager.remoteCheckerForTask
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.invokeLater
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.NonNls
import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Supplier

class RetryAction(
  actionText: Supplier<@ActionText String>,
  private val processMessage: String = PROCESS_MESSAGE,
  private val expectedTaskStatus: CheckStatus = CheckStatus.Failed
) : ActionWithProgressIcon(actionText),
    DumbAware {

  constructor() : this(EduCoreBundle.lazyMessage("retry")) {
    setUpSpinnerPanel(processMessage)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return

    if (!task.isChangedOnFailed || task.status != expectedTaskStatus) {
      return
    }

    if (!RetryActionState.getInstance(project).doLock()) {
      e.dataContext.showPopup(EduCoreBundle.message("action.retry.already.running"))
      return
    }
    val retryTask = RetryTask(project, task, e.dataContext)
    if (retryTask.isHeadless) {
      /**
       *  [CheckAction]:122
       */
      val future = ApplicationManager.getApplication().executeOnPooledThread { ProgressManager.getInstance().run(retryTask) }
      EduActionUtils.waitAndDispatchInvocationEvents(future)
    }
    else {
      ProgressManager.getInstance().run(retryTask)
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    val project = e.project ?: return
    val task = project.getCurrentTask() ?: return
    if (task.status != expectedTaskStatus) return
  }

  private inner class RetryTask(
    project: Project,
    private val task: Task,
    private val context: DataContext
  ) : com.intellij.openapi.progress.Task.Backgroundable(
    project,
    EduCoreBundle.message("action.retry.task.background"), true
  ) {
    private lateinit var result: Result<Boolean, String>

    override fun run(indicator: ProgressIndicator) {
      invokeAndWaitIfNeeded {
        processStarted()
      }
      ApplicationManager.getApplication().executeOnPooledThread { EduActionUtils.showFakeProgress(indicator) }
      val remoteChecker = remoteCheckerForTask(project, task) ?: return
      result = remoteChecker.retry(task) ?: return
    }

    override fun onSuccess() {
      when (val res = result) {
        is Ok -> {}
        is Err -> if (!isHeadless) {
          context.showPopup(res.error)
        }
      }
    }

    override fun onFinished() {
      resetTaskStatus()
      project.invokeLater {
        TaskToolWindowView.getInstance(project).updateTaskSpecificPanel()
        TaskToolWindowView.getInstance(project).updateCheckPanel(task)
      }
      processFinished()
      RetryActionState.getInstance(project).unlock()
    }

    private fun resetTaskStatus() {
      task.status = CheckStatus.Unchecked
      YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
    }
  }

  @Service(Service.Level.PROJECT)
  private class RetryActionState {
    private val isBusy = AtomicBoolean(false)
    fun doLock(): Boolean {
      return isBusy.compareAndSet(false, true)
    }

    fun unlock() {
      isBusy.set(false)
    }

    companion object {
      fun getInstance(project: Project): RetryActionState {
        return project.service()
      }
    }
  }

  companion object {
    @NonNls
    const val ACTION_ID: String = "HyperskillEducational.Retry"

    @NonNls
    private const val PROCESS_MESSAGE: String = "Retry in progress"
  }
}
