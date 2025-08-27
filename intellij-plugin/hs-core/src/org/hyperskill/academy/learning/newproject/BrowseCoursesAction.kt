package org.hyperskill.academy.learning.newproject

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.hyperskill.academy.learning.RemoteEnvHelper
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.ui.BrowseCoursesDialog
import org.jetbrains.annotations.NonNls

class BrowseCoursesAction : DumbAwareAction(
  EduCoreBundle.message("browse.courses"),
  EduCoreBundle.message("browse.courses.description"),
  null
) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = !RemoteEnvHelper.isRemoteDevServer()
  }

  override fun actionPerformed(e: AnActionEvent) {
    BrowseCoursesDialog().show()
  }

  companion object {
    @NonNls
    const val ACTION_ID = "HyperskillEducational.BrowseCourses"
  }
}
