package org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers

import org.hyperskill.academy.learning.courseFormat.tasks.Task

interface TaskResourcesManager<T : Task> {

  fun getText(task: T): String
}