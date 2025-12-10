package org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.hyperskill.academy.learning.EduLogInListener
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.HyperskillCourseAdvertiser
import org.hyperskill.academy.learning.newproject.ui.CourseCardComponent
import org.hyperskill.academy.learning.newproject.ui.CoursesPanel
import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import org.hyperskill.academy.learning.stepik.hyperskill.JBA_HELP
import org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI.notLoggedInPanel.HyperskillNotLoggedInPanel
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import javax.swing.JPanel

private val LOG = logger<HyperskillCoursesPanel>()

class HyperskillCoursesPanel(
  private val platformProvider: HyperskillPlatformProvider,
  val scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  override fun tabDescription(): String {
    val linkText = """<a href="$JBA_HELP">${EduNames.JBA}</a>"""
    return EduCoreBundle.message("hyperskill.courses.explanation", linkText)
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: JBACourseFromStorage) {
    val firstGroup = coursesGroups.firstOrNull()
    if (firstGroup != null) {
      if (firstGroup.courses.none { it.id != deletedCourse.id }) {
        coursesGroups[0] = firstGroup.copy(courses = listOf(HyperskillCourseAdvertiser()))
      }
    }
    super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
  }

  override fun createNoCoursesPanel(): JPanel = if (isLoggedIn()) {
    HyperskillSelectTrackPanel()
  }
  else {
    HyperskillNotLoggedInPanel()
  }

  override fun createCoursesListPanel() = HyperskillCoursesListPanel()

  override fun createContentPanel(): JPanel {
    LOG.info("createContentPanel called, isLoggedIn=${isLoggedIn()}")
    val panel = if (isLoggedIn()) {
      super.createContentPanel()
    }
    else {
      HyperskillNotLoggedInPanel()
    }

    fun createCoursesPanel() = super.createContentPanel()

    val connection = ApplicationManager.getApplication().messageBus.connect()
    LOG.info("Subscribing to LOGGED_IN_TO_HYPERSKILL topic")
    connection.subscribe(
      HyperskillSettings.LOGGED_IN_TO_HYPERSKILL,
      object : EduLogInListener {
        override fun userLoggedIn() {
          LOG.info("userLoggedIn event received in HyperskillCoursesPanel, panel.isDisplayable=${panel.isDisplayable}, panel.isShowing=${panel.isShowing}")
          runInEdt(ModalityState.any()) {
            LOG.info("Updating panel after login: removing old content and adding courses panel")
            if (!panel.isDisplayable) {
              LOG.warn("Panel is not displayable, skipping update")
              connection.disconnect()
              return@runInEdt
            }
            panel.removeAll()
            panel.add(createCoursesPanel())
            panel.revalidate()
            panel.repaint()
            showContent(false)
            scope.launch {
              updateCoursesAfterLogin(false)
            }
            connection.disconnect()
            LOG.info("Panel update completed, connection disconnected")
          }
        }
      }
    )

    return panel
  }

  override suspend fun updateCoursesAfterLogin(preserveSelection: Boolean) {
    val academyCoursesGroups = withContext(Dispatchers.IO) { platformProvider.loadCourses() }
    coursesGroups.clear()
    coursesGroups.addAll(academyCoursesGroups)
    super.updateCoursesAfterLogin(false)
  }

  private fun isLoggedIn() = HyperskillSettings.INSTANCE.account != null

  inner class HyperskillCoursesListPanel : CoursesListWithResetFilters() {
    override fun createCardForNewCourse(course: Course): CourseCardComponent {
      return HyperskillCourseCard(course)
    }
  }
}