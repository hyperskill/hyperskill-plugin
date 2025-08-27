package org.hyperskill.academy.learning.actions

import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.jetbrains.annotations.NonNls

class PreviousTaskAction : TaskNavigationAction() {

  override fun getTargetTask(sourceTask: Task): Task? = NavigationUtils.previousTask(sourceTask)

  companion object {
    @NonNls
    const val ACTION_ID = "HyperskillEducational.PreviousTask"
  }
}
