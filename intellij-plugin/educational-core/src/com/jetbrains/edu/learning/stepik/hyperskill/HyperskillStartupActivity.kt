package com.jetbrains.edu.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.backend.observation.trackActivity
import com.jetbrains.edu.learning.EduCourseConfigurationActivityKey
import com.jetbrains.edu.learning.EduLogInListener
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.StudyTaskManager
import com.jetbrains.edu.learning.courseFormat.hyperskill.HyperskillCourse
import com.jetbrains.edu.learning.isUnitTestMode
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillConnector
import com.jetbrains.edu.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import com.jetbrains.edu.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import com.jetbrains.edu.learning.submissions.SubmissionsManager
import com.jetbrains.edu.learning.taskToolWindow.ui.TaskToolWindowView
import com.jetbrains.edu.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer

class HyperskillStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) = project.trackActivity(EduCourseConfigurationActivityKey) {
    if (project.isDisposed || !project.isStudentProject() || isUnitTestMode) return@trackActivity

    val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return@trackActivity
    val submissionsManager = SubmissionsManager.getInstance(project)
    if (!submissionsManager.submissionsSupported()) return@trackActivity

    submissionsManager.prepareSubmissionsContentWhenLoggedIn {
      HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground()
    }

    HyperskillConnector.getInstance().setSubmissionTabListener(object : EduLogInListener {
      override fun userLoggedIn() {
        if (project.isDisposed) return
        submissionsManager.prepareSubmissionsContentWhenLoggedIn()
      }

      override fun userLoggedOut() {
        if (project.isDisposed) return
        TaskToolWindowView.getInstance(project).updateTab(SUBMISSIONS_TAB)
      }
    })

    synchronizeTopics(project, course)
    HyperskillCourseUpdateChecker.getInstance(project).check()
  }

}

fun synchronizeTopics(project: Project, hyperskillCourse: HyperskillCourse) {
  ApplicationManager.getApplication().executeOnPooledThread {
    HyperskillConnector.getInstance().fillTopics(project, hyperskillCourse)
    YamlFormatSynchronizer.saveRemoteInfo(hyperskillCourse)
  }
}
