package org.hyperskill.academy.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.actions.SyncCourseAction
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import org.jetbrains.annotations.NonNls

class SyncHyperskillCourseAction : SyncCourseAction(
  EduCoreBundle.lazyMessage("hyperskill.update.project"),
  EduCoreBundle.lazyMessage("hyperskill.update.project"), null
) {

  override val loginWidgetText: String
    get() = EduCoreBundle.message("hyperskill.action.synchronize.project")

  override fun synchronizeCourse(project: Project) {
    val course = project.course as HyperskillCourse
    HyperskillCourseUpdater(project, course).updateCourse { isUpdated ->
      if (!isUpdated) {
        EduNotificationManager.showInfoNotification(
          project,
          EduCoreBundle.message("update.nothing.to.update"),
          EduCoreBundle.message("update.notification.text", EduNames.JBA, EduNames.PROJECT),
        )
      }

      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
    }

  }

  override fun isAvailable(project: Project): Boolean {
    return project.course is HyperskillCourse
  }

  companion object {
    @NonNls
    const val ACTION_ID = "HyperskillEducational.UpdateCourse"
  }
}