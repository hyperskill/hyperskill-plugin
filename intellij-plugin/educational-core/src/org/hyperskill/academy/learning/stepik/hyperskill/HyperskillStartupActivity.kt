package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.backend.observation.trackActivity
import org.hyperskill.academy.learning.EduCourseConfigurationActivityKey
import org.hyperskill.academy.learning.EduLogInListener
import org.hyperskill.academy.learning.EduUtilsKt.isStudentProject
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import org.hyperskill.academy.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer

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
