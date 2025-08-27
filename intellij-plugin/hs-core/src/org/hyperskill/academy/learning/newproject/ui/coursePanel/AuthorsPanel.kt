package org.hyperskill.academy.learning.newproject.ui.coursePanel

import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.newproject.ui.GrayTextHtmlPanel


private const val INFO_PANEL_TOP_OFFSET = 7

class AuthorsPanel : JBScrollPane(
  VERTICAL_SCROLLBAR_NEVER,
  HORIZONTAL_SCROLLBAR_NEVER
), CourseSelectionListener {
  private var authorsLabel: GrayTextHtmlPanel = GrayTextHtmlPanel("text").apply {
    border = JBUI.Borders.empty(INFO_PANEL_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
  }

  init {
    setViewportView(authorsLabel)
    border = JBUI.Borders.empty()
  }

  override fun onCourseSelectionChanged(data: CourseBindData) {
    val (course, courseDisplaySettings) = data
    val allAuthors = course.authorFullNames.joinToString()
    isVisible = courseDisplaySettings.showInstructorField && allAuthors.isNotEmpty()
    if (authorsLabel.isVisible) {
      authorsLabel.setBody(allAuthors)
    }
  }
}
