package org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.ui.Messages
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.computeUnderProgress
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.CourseCreationInfo
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.newproject.ui.CoursesPanel
import org.hyperskill.academy.learning.newproject.ui.CoursesPlatformProvider
import org.hyperskill.academy.learning.newproject.ui.CoursesPlatformProviderFactory
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CoursePanel
import org.hyperskill.academy.learning.newproject.ui.coursePanel.groups.CoursesGroup
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenProjectStageRequest
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillProjectAction
import javax.swing.Icon

class HyperskillPlatformProviderFactory : CoursesPlatformProviderFactory {
  override fun getProviders(): List<CoursesPlatformProvider> = listOf(HyperskillPlatformProvider())
}

class HyperskillPlatformProvider : CoursesPlatformProvider() {
  private val LOG = logger<HyperskillPlatformProvider>()

  override val name: String = EduNames.JBA

  override val icon: Icon get() = EducationalCoreIcons.Platform.Tab.HyperskillAcademyTab

  override fun createPanel(scope: CoroutineScope, disposable: Disposable): CoursesPanel = HyperskillCoursesPanel(
    this, scope,
    disposable
  )

  override fun joinAction(
    courseInfo: CourseCreationInfo,
    courseMode: CourseMode,
    coursePanel: CoursePanel,
    openCourseParams: Map<String, String>
  ) {
    val course = courseInfo.course
    LOG.info("joinAction called for course: ${course.name}, type: ${course.javaClass.simpleName}")
    val startTime = System.currentTimeMillis()

    if (course is HyperskillCourse) {
      // If hyperskillProject is not set (e.g., course created from storage), load it from API
      if (course.hyperskillProject == null) {
        LOG.info("hyperskillProject is null for course ID: ${course.id}, loading from API")
        val projectResult = computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.project.info")) {
          HyperskillConnector.getInstance().getProject(course.id)
        }
        when (projectResult) {
          is Ok -> {
            course.hyperskillProject = projectResult.value
            LOG.info("hyperskillProject loaded successfully: projectId=${projectResult.value.id}, title=${projectResult.value.title}")
          }
          is Err -> {
            val errorMessage = "Failed to load project information: ${projectResult.error}"
            Messages.showErrorDialog(errorMessage, EduCoreBundle.message("hyperskill.failed.to.open.project"))
            LOG.warn("Failed to load hyperskillProject for course ID ${course.id}: ${projectResult.error}")
            return
          }
        }
      }

      LOG.info("Loading stages for HyperskillCourse via joinAction")
      computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.stages")) {
        HyperskillConnector.getInstance().loadStages(course)
      }
      LOG.info("Stages loaded in ${System.currentTimeMillis() - startTime}ms, proceeding to super.joinAction")
      super.joinAction(courseInfo, courseMode, coursePanel, openCourseParams)
      LOG.info("joinAction completed in ${System.currentTimeMillis() - startTime}ms total")
      return
    }

    LOG.info("Opening Hyperskill project via HyperskillProjectAction")
    val isOpened = HyperskillProjectAction.openHyperskillProject { errorMessage ->
      Messages.showErrorDialog(errorMessage.message, EduCoreBundle.message("hyperskill.failed.to.open.project"))
      LOG.warn("Joining a course resulted in an error: ${errorMessage.message}. The error was shown inside an error dialog.")
    }
    LOG.info("HyperskillProjectAction.openHyperskillProject returned: $isOpened in ${System.currentTimeMillis() - startTime}ms")

    if (isOpened) {
      val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, coursePanel)
      dialog?.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
    }
  }

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    val selectedProject = getSelectedProject()
    val storedCourses = CoursesStorage.getInstance().state.courses
      .filter { it.type == HYPERSKILL && it.id != selectedProject?.id }
      .map { it.toCourse() }

    val courses = run { listOfNotNull(selectedProject?.course) + storedCourses }

    return if (courses.isNotEmpty()) CoursesGroup.fromCourses(courses) else emptyList()
  }

  private val HyperskillProject.course: HyperskillCourse?
    get() = HyperskillOpenInIdeRequestHandler.createHyperskillCourse(
      HyperskillOpenProjectStageRequest(id, null),
      language,
      this
    ).onError { null }

  private fun getSelectedProject(): HyperskillProject? {
    val currentUserInfo = HyperskillConnector.getInstance().getCurrentUserInfo() ?: return null
    val projectId = currentUserInfo.hyperskillProjectId ?: return null

    return HyperskillConnector.getInstance().getProject(projectId).onError {
      return null
    }
  }
}