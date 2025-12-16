package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.platform.backend.observation.trackActivity
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.EduCourseConfigurationActivityKey
import org.hyperskill.academy.learning.EduLogInListener
import org.hyperskill.academy.learning.RefreshCause
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ALL_FILES
import org.hyperskill.academy.learning.guessCourseDir
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.runInBackground
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import org.hyperskill.academy.learning.stepik.hyperskill.update.HyperskillCourseUpdateChecker
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TabType.SUBMISSIONS_TAB
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer

class HyperskillStartupActivity : ProjectActivity {

  override suspend fun execute(project: Project) = project.trackActivity(EduCourseConfigurationActivityKey) {
    if (project.isDisposed || isUnitTestMode) return@trackActivity

    val course = StudyTaskManager.getInstance(project).course as? HyperskillCourse ?: return@trackActivity

    // Check if course directory exists, restore if needed
    val courseDir = project.guessCourseDir()
    if (courseDir == null || !courseDir.isValid) {
      LOG.info("Course directory is missing or invalid, attempting to restore course structure")
      restoreCourseStructure(project, course)
      return@trackActivity
    }

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

  companion object {
    private val LOG = logger<HyperskillStartupActivity>()
  }
}

private fun restoreCourseStructure(project: Project, course: HyperskillCourse) {
  runInBackground(project, EduCoreBundle.message("hyperskill.loading.project"), false) {
    val connector = HyperskillConnector.getInstance()

    // Load stages from server
    connector.loadStages(course)
    course.init(false)

    val projectLesson = course.getProjectLesson()
    if (projectLesson == null) {
      LOG.warn("Failed to restore course: project lesson is null after loading stages")
      return@runInBackground
    }

    val courseDir = project.courseDir
    GeneratorUtils.createLesson(project, projectLesson, courseDir)
    GeneratorUtils.unpackAdditionalFiles(CourseInfoHolder.fromCourse(course, courseDir), ALL_FILES)
    YamlFormatSynchronizer.saveAll(project)
    course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.DEPENDENCIES_UPDATED)

    LOG.info("Course structure restored successfully")
  }
}

private val LOG = logger<HyperskillStartupActivity>()

fun synchronizeTopics(project: Project, hyperskillCourse: HyperskillCourse) {
  ApplicationManager.getApplication().executeOnPooledThread {
    HyperskillConnector.getInstance().fillTopics(project, hyperskillCourse)
    // Skip saving if course directory was deleted or is invalid
    val courseDir = project.guessCourseDir()
    if (courseDir == null || !courseDir.isValid) return@executeOnPooledThread
    YamlFormatSynchronizer.saveRemoteInfo(hyperskillCourse)
  }
}
