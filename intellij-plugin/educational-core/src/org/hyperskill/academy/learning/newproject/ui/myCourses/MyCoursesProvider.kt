package org.hyperskill.academy.learning.newproject.ui.myCourses

import com.intellij.openapi.Disposable
import com.intellij.ui.ColorUtil
import com.intellij.ui.components.JBLabel
import com.intellij.ui.scale.JBUIScale
import com.intellij.util.ui.UIUtil
import kotlinx.coroutines.CoroutineScope
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.newproject.ui.CoursesPanel
import org.hyperskill.academy.learning.newproject.ui.CoursesPlatformProvider
import org.hyperskill.academy.learning.newproject.ui.GRAY_COLOR
import org.hyperskill.academy.learning.newproject.ui.coursePanel.groups.CoursesGroup
import javax.swing.Icon

class MyCoursesProvider : CoursesPlatformProvider() {
  override val name: String get() = EduCoreBundle.message("course.dialog.my.courses")

  override val icon: Icon? = null

  override fun createPanel(scope: CoroutineScope, disposable: Disposable): CoursesPanel = MyCoursesPanel(this, scope, disposable)

  override suspend fun doLoadCourses(): List<CoursesGroup> {
    return CoursesStorage.getInstance().coursesInGroups()
  }

  fun getAdditionalText(isSelected: Boolean): String {
    val courses = CoursesStorage.getInstance().state.courses
    val studyCourses = courses.filter { it.courseMode == CourseMode.STUDENT }
    val completedCourses = studyCourses.count { it.tasksTotal != 0 && it.tasksSolved == it.tasksTotal }
    val inProgressCourses = studyCourses.size - completedCourses

    val additionalText = if (completedCourses != 0) {
      EduCoreBundle.message("course.dialog.my.courses.additional.text.full", inProgressCourses, completedCourses)
    }
    else {
      EduCoreBundle.message("course.dialog.my.courses.additional.text", inProgressCourses)
    }
    val additionalTextFontSize = JBLabel().font.size - 2
    val color = ColorUtil.toHex(if (isSelected) UIUtil.getTreeSelectionForeground(true) else GRAY_COLOR)
    val style = "font-size: $additionalTextFontSize; color: #$color; margin-top: ${JBUIScale.scale(4)}px"
    return """<div style="$style">$additionalText</div>"""
  }
}