package org.hyperskill.academy.learning.stepik.hyperskill.widget

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBarWidget
import org.hyperskill.academy.learning.LoginWidgetFactory
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.messages.EduCoreBundle

class HyperskillWidgetFactory : LoginWidgetFactory() {
  override val widgetId: String = "widget.hyperskill"

  override fun isWidgetAvailable(course: Course) = course is HyperskillCourse

  override fun getDisplayName(): String = EduCoreBundle.message("hyperskill.widget.title")

  override fun createWidget(project: Project): StatusBarWidget = HyperskillWidget(project)
}
