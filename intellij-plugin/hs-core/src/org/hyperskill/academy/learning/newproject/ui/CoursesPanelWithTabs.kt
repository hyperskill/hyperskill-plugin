package org.hyperskill.academy.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.JBCardLayout
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.hyperskill.academy.learning.LanguageSettings
import org.hyperskill.academy.learning.newproject.coursesStorage.CourseDeletedListener
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorageBase
import org.hyperskill.academy.learning.newproject.ui.myCourses.MyCoursesProvider
import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import java.awt.BorderLayout
import javax.swing.JPanel

private const val PANEL_WIDTH = 1050
private const val PANEL_HEIGHT = 750

class CoursesPanelWithTabs(private val scope: CoroutineScope, private val disposable: Disposable, val isPreferredSize: Boolean = true) :
  JPanel() {
  private val coursesTab: CoursesTab
  private val myCoursesProvider: MyCoursesProvider = MyCoursesProvider()

  val languageSettings: LanguageSettings<*>? get() = coursesTab.languageSettings()

  init {
    layout = BorderLayout()
    coursesTab = CoursesTab()
    add(coursesTab, BorderLayout.CENTER)
    if (isPreferredSize) {
      preferredSize = JBUI.size(PANEL_WIDTH, PANEL_HEIGHT)
    }
  }

  fun doValidation() {
    coursesTab.doValidation()
  }

  fun loadCourses() {
    coursesTab.loadCourses(scope)
  }

  private inner class CoursesTab : JPanel() {
    private val panels: MutableList<CoursesPanel> = mutableListOf()
    private var activeTabName: String? = null
    private val cardLayout = JBCardLayout()

    init {
      layout = cardLayout
      val providers = CoursesPlatformProviderFactory.allProviders
      providers.forEach { provider ->
        addPanel(provider)
      }

      addPanel(myCoursesProvider)
      showPanel(providers.first().name)
      val connection = ApplicationManager.getApplication().messageBus.connect(disposable)
      connection.subscribe(CoursesStorageBase.COURSE_DELETED, object : CourseDeletedListener {
        override fun courseDeleted(course: JBACourseFromStorage) {
          panels.forEach {
            it.updateModelAfterCourseDeletedFromStorage(course)
          }
        }
      })
    }

    private fun addPanel(coursesPlatformProvider: CoursesPlatformProvider) {
      val panel = coursesPlatformProvider.createPanel(scope, disposable)
      panels.add(panel)
      add(coursesPlatformProvider.name, panel)
    }

    fun loadCourses(scope: CoroutineScope) {
      panels.forEach {
        scope.launch {
          it.loadCourses()
        }
      }
    }

    fun showPanel(name: String) {
      activeTabName = name
      val panel = cardLayout.findComponentById(activeTabName) as? CoursesPanel ?: return
      panel.onTabSelection()
      cardLayout.show(this, activeTabName)
      val focusManager = IdeFocusManager.findInstanceByComponent(panel)
      val toFocus = focusManager.getFocusTargetFor(panel) ?: return
      ApplicationManager.getApplication().invokeLater({ focusManager.requestFocus(toFocus, true) }, ModalityState.current())
    }

    fun doValidation() {
      (cardLayout.findComponentById(activeTabName) as CoursesPanel).doValidation()
    }

    fun languageSettings() = currentPanel.languageSettings

    private val currentPanel: CoursesPanel
      get() {
        activeTabName ?: error("Active tab name is null")
        val activeComponent = (layout as JBCardLayout).findComponentById(activeTabName)
        return activeComponent as CoursesPanel
      }
  }
}
