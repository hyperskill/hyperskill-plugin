package org.hyperskill.academy.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.newproject.coursesStorage.CourseDeletedListener
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorageBase
import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import java.awt.BorderLayout
import javax.swing.JPanel

private const val PANEL_WIDTH = 1050
private const val PANEL_HEIGHT = 750

class CoursesPanelWithTabs(
  private val scope: CoroutineScope,
  private val disposable: Disposable,
  isPreferredSize: Boolean = true
) : JPanel() {

  private val coursesPanel: CoursesPanel

  val languageSettings: LanguageSettings<*>? get() = coursesPanel.languageSettings

  init {
    layout = BorderLayout()
    val provider = CoursesPlatformProviderFactory.allProviders.first()
    coursesPanel = provider.createPanel(scope, disposable)
    add(coursesPanel, BorderLayout.CENTER)

    if (isPreferredSize) {
      preferredSize = JBUI.size(PANEL_WIDTH, PANEL_HEIGHT)
    }

    val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
    connection.subscribe(CoursesStorageBase.COURSE_DELETED, object : CourseDeletedListener {
      override fun courseDeleted(course: JBACourseFromStorage) {
        coursesPanel.updateModelAfterCourseDeletedFromStorage(course)
      }
    })
  }

  fun doValidation() {
    coursesPanel.doValidation()
  }

  fun loadCourses() {
    scope.launch {
      coursesPanel.loadCourses()
    }
  }
}
