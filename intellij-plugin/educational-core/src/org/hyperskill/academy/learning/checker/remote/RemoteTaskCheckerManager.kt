package org.hyperskill.academy.learning.checker.remote

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.tasks.Task

object RemoteTaskCheckerManager {
  private val EP_NAME = ExtensionPointName.create<RemoteTaskChecker>("HyperskillEducational.remoteTaskChecker")

  fun remoteCheckerForTask(project: Project, task: Task): RemoteTaskChecker? {
    val checkers = EP_NAME.extensionList.filter { it.canCheck(project, task) }
    if (checkers.isEmpty()) {
      return null
    }
    if (checkers.size > 1) {
      error("Several remote task checkers available for ${task.itemType}:${task.name}: $checkers")
    }
    return checkers[0]
  }

}