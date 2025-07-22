package org.hyperskill.academy.learning.newproject.ui.coursePanel

import com.intellij.util.ui.HtmlPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.messages.EduCoreBundle

abstract class CourseHtmlPanel : HtmlPanel() {
  protected var course: Course? = null

  init {
    background = UIUtil.getEditorPaneBackground()
    isFocusable = false
    border = JBUI.Borders.empty()

    // set some text to force JEditorPane calculate its height properly
    text = EduCoreBundle.message("course.dialog.no.course.selected")
  }

  fun bind(course: Course) {
    this.course = course
    update()
  }
}