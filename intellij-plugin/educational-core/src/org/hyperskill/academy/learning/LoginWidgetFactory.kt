package org.hyperskill.academy.learning

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsManager
import org.hyperskill.academy.coursecreator.CCUtils.isLocalCourse
import org.hyperskill.academy.learning.EduUtilsKt.isEduProject
import org.hyperskill.academy.learning.courseFormat.Course

abstract class LoginWidgetFactory : StatusBarWidgetFactory {
  protected abstract val widgetId: String

  override fun getId(): String = widgetId

  override fun canBeEnabledOn(statusBar: StatusBar): Boolean = true

  override fun isAvailable(project: Project): Boolean {
    if (!project.isEduProject()) return false
    val course = project.service<StudyTaskManager>().course

    if (course == null) {
      project.messageBus.connect().subscribe(StudyTaskManager.COURSE_SET, object : CourseSetListener {
        override fun courseSet(course: Course) {
          val statusBarWidgetsManager = project.service<StatusBarWidgetsManager>()
          statusBarWidgetsManager.updateWidget(this@LoginWidgetFactory)
        }
      })
      return false
    }

    return !project.isLocalCourse && isWidgetAvailable(course)
  }

  override fun disposeWidget(widget: StatusBarWidget) = Disposer.dispose(widget)

  abstract fun isWidgetAvailable(course: Course): Boolean
}