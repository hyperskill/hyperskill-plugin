package org.hyperskill.academy.learning.stepik.hyperskill.metrics

import com.intellij.openapi.application.ApplicationActivationListener
import com.intellij.openapi.wm.IdeFrame
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.selectedTaskFile

class HyperskillMetricsApplicationActivationListener : ApplicationActivationListener {
  override fun applicationActivated(ideFrame: IdeFrame) {
    val project = ideFrame.project ?: return
    val task = project.selectedTaskFile?.task ?: return
    val course = task.course
    if (course !is HyperskillCourse) {
      return
    }
    HyperskillMetricsService.getInstance().taskStarted(task)
  }

  override fun applicationDeactivated(ideFrame: IdeFrame) {
    HyperskillMetricsService.getInstance().taskStopped()
  }
}