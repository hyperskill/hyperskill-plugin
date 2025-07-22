package org.hyperskill.academy.shell

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.sh.shellcheck.ShShellcheckUtil
import com.intellij.ui.EditorNotifications
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings

class ShellCourseProjectGenerator(
  builder: EduCourseBuilder<EmptyProjectSettings>,
  course: Course
) : CourseProjectGenerator<EmptyProjectSettings>(builder, course) {
  override fun afterProjectGenerated(
    project: Project,
    projectSettings: EmptyProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    val onSuccess = Runnable {
      runReadAction {
        if (project.isDisposed) return@runReadAction
        EditorNotifications.getInstance(project).updateAllNotifications()
      }
    }
    val onFailure = Runnable { }
    ShShellcheckUtil.download(project, onSuccess, onFailure)
    super.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)
  }
}