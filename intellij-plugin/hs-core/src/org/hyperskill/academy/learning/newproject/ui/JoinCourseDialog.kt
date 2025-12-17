package org.hyperskill.academy.learning.newproject.ui

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.newproject.CourseCreationInfo
import org.hyperskill.academy.learning.newproject.coursesStorage.CourseDeletedListener
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorageBase
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CourseBindData
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CourseDisplaySettings
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CoursePanel
import org.hyperskill.academy.learning.newproject.ui.errors.ErrorState
import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import javax.swing.JComponent

open class JoinCourseDialog(
  protected val course: Course,
  protected val settings: CourseDisplaySettings = CourseDisplaySettings(),
  private val params: Map<String, String> = emptyMap()
) : OpenCourseDialogBase() {

  init {
    super.init()
    title = course.name
  }

  override fun createCenterPanel(): JComponent {
    val panel = createCoursePanel()
    panel.bindCourse(CourseBindData(course, settings))
    panel.preferredSize = JBUI.size(500, 530)
    return panel
  }

  protected open fun createCoursePanel(): CoursePanel {
    return JoinCoursePanel(disposable)
  }

  protected open fun isToShowError(errorState: ErrorState): Boolean = true

  private inner class JoinCoursePanel(parentDisposable: Disposable) : CoursePanel(parentDisposable, true) {
    init {
      // Subscribe to course deletion events to update button state
      val connection = ApplicationManager.getApplication().messageBus.connect(parentDisposable)
      connection.subscribe(CoursesStorageBase.COURSE_DELETED, object : CourseDeletedListener {
        override fun courseDeleted(deletedCourse: JBACourseFromStorage) {
          // If the deleted course is the one we're showing, update button visibility
          if (deletedCourse.id == this@JoinCourseDialog.course.id) {
            bindCourse(CourseBindData(this@JoinCourseDialog.course, settings))
          }
        }
      })
    }

    override fun joinCourseAction(info: CourseCreationInfo, mode: CourseMode) {
      CoursesPlatformProvider.joinCourse(info, mode, this, params) {
        setError(it)
      }
    }

    override fun openCourseMetadata(): Map<String, String> = params

    override fun showError(errorState: ErrorState) {
      if (isToShowError(errorState)) {
        super.showError(errorState)
      }
    }
  }
}
