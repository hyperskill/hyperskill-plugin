package org.hyperskill.academy.learning.stepik.hyperskill.newProjectUI

import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.HyperskillCourseAdvertiser
import org.hyperskill.academy.learning.newproject.ui.CourseCardComponent
import org.hyperskill.academy.learning.newproject.ui.CoursesDialogFontManager
import org.hyperskill.academy.learning.newproject.ui.GRAY_COLOR
import org.hyperskill.academy.learning.taskToolWindow.ui.styleManagers.TypographyManager
import java.awt.FlowLayout
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel

private const val INFO_HGAP = 0
private const val INFO_VGAP = 5

class HyperskillCourseCard(course: Course) : CourseCardComponent(course) {

  override fun createBottomComponent(): JPanel {
    return AcademyCourseInfoComponent(course is HyperskillCourseAdvertiser)
  }
}

private class AcademyCourseInfoComponent(isAdvertisingCourse: Boolean) : JPanel(FlowLayout(FlowLayout.LEFT, INFO_HGAP, INFO_VGAP)) {

  init {
    val commentLabel = JLabel().apply {
      foreground = GRAY_COLOR
      font = Font(TypographyManager().bodyFont, Font.PLAIN, CoursesDialogFontManager.smallCardFontSize)
      if (isAdvertisingCourse) {
        text = EduCoreBundle.message("course.dialog.jba.default.card.info")
      }
    }

    add(commentLabel)
  }
}