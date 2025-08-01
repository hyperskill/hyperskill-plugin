package org.hyperskill.academy.learning.actions

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView

abstract class TaskNavigationAction : DumbAwareAction() {

  protected open fun getCustomAction(task: Task): ((Project, Task) -> Unit)? = null

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    if (!project.isEduProject()) return
    navigateTask(project, e.place)
  }

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabled = false
    val project = e.project ?: return
    if (!project.isEduProject()) return
    val currentTask = TaskToolWindowView.getInstance(project).currentTask ?: return
    if (getTargetTask(currentTask) != null || getCustomAction(currentTask) != null) {
      e.presentation.isEnabled = true
    }
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  private fun navigateTask(project: Project, place: String) {
    val currentTask = TaskToolWindowView.getInstance(project).currentTask ?: return
    val customAction = getCustomAction(currentTask)
    if (customAction != null) {
      customAction(project, currentTask)
      return
    }
    val targetTask = getTargetTask(currentTask) ?: return

    NavigationUtils.navigateToTask(project, targetTask, currentTask)
  }

  protected abstract fun getTargetTask(sourceTask: Task): Task?
}
