package org.hyperskill.academy.learning.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.stepik.hyperskill.openNextActivity
import org.hyperskill.academy.learning.taskToolWindow.ui.check.CheckPanel
import org.jetbrains.annotations.NonNls

class NextTaskAction : TaskNavigationAction() {

  override fun getTargetTask(sourceTask: Task): Task? = NavigationUtils.nextTask(sourceTask)

  override fun update(e: AnActionEvent) {
    if (CheckPanel.ACTION_PLACE == e.place) {
      //action is being added only in valid context
      //no project in event in this case, so just enable it
      return
    }
    super.update(e)
  }

  override fun getCustomAction(task: Task): ((Project, Task) -> Unit)? {
    return if (NavigationUtils.isLastHyperskillProblem(task)) ::openNextActivity else null
  }

  companion object {
    @NonNls
    const val ACTION_ID = "HyperskillEducational.NextTask"
  }
}
