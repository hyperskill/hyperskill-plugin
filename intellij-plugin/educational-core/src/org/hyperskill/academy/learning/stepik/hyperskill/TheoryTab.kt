package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindow.Companion.getTaskDescription
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TaskToolWindowTextTab

/**
 * Constructor is called exclusively in [org.hyperskill.academy.learning.taskToolWindow.ui.tab.TabManager.createTab]
 * and MUST NOT be called in any other places
 */
class TheoryTab(project: Project) : TaskToolWindowTextTab(project) {

  init {
    init()
  }

  override fun update(task: Task) {
    if (task !is TheoryTask) {
      error("Selected task isn't Theory task")
    }

    setText(getTaskDescription(project, task, uiMode))
  }
}