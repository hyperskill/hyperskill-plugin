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
  private val scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(platformProvider, scope, disposable) {

  private var contentPanel: JPanel? = null

  init {
    subscribeToLoginEvents(disposable)
  }

  private fun subscribeToLoginEvents(disposable: Disposable) {
    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(
      HyperskillSettings.LOGGED_IN_TO_HYPERSKILL,
      object : EduLogInListener {
        override fun userLoggedIn() {
          LOG.info("userLoggedIn event received")
          runInEdt(ModalityState.any()) {
            onUserLoggedIn()
          }
        }
      }
    )
  }

  private fun onUserLoggedIn() {
    LOG.info("Updating panels after login")

    // Update content panel
    contentPanel?.let { panel ->
      if (panel.isDisplayable) {
        panel.removeAll()
        panel.add(createCoursesContentPanel())
        panel.revalidate()
        panel.repaint()
      }
    }

    // Update no courses panel (it may show different content based on login state)
    updateNoCoursesPanel()

    // Load courses and update UI
    scope.launch {
      updateCoursesAfterLogin(false)
    }
    LOG.info("Panel update completed")
  }

  override fun tabDescription(): String {
    val linkText = """<a href="$JBA_HELP">${EduNames.JBA}</a>"""
    return EduCoreBundle.message("hyperskill.courses.explanation", linkText)
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: JBACourseFromStorage) {
    scope.launch {
      val reloadedGroups = withContext(Dispatchers.IO) { platformProvider.loadCourses() }
      coursesGroups.clear()
      coursesGroups.addAll(reloadedGroups)
      super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
      showContent(coursesGroups.isEmpty())
    }
  }

  override fun createNoCoursesPanel(): JPanel = HyperskillSelectTrackPanel()

  override fun createCoursesListPanel() = HyperskillCoursesListPanel()

  override fun createContentPanel(): JPanel {
    LOG.info("createContentPanel called, isLoggedIn=${isLoggedIn()}")
    val panel = if (isLoggedIn()) {
      createCoursesContentPanel()
    }
    else {
      HyperskillNotLoggedInPanel()
    }
    contentPanel = panel
    return panel
  }

  private fun createCoursesContentPanel(): JPanel = super.createContentPanel()

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