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
  override val name: String = EduNames.JBA

  override val icon: Icon get() = EducationalCoreIcons.Platform.Tab.JetBrainsAcademyTab

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
    if (course is HyperskillCourse) {
      computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.stages")) {
        HyperskillConnector.getInstance().loadStages(course)
      }
      super.joinAction(courseInfo, courseMode, coursePanel, openCourseParams)
      return
    }

    val isOpened = HyperskillProjectAction.openHyperskillProject { errorMessage ->
      Messages.showErrorDialog(errorMessage.message, EduCoreBundle.message("hyperskill.failed.to.open.project"))
      logger<HyperskillPlatformProvider>().warn("Joining a course resulted in an error: ${errorMessage.message}. The error was shown inside an error dialog.")
    }

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