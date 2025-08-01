package org.hyperskill.academy.learning.newproject.ui.coursePanel

import com.intellij.ide.plugins.newui.HorizontalLayout
import com.intellij.ide.plugins.newui.TagComponent
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.tags


private const val TAGS_TOP_OFFSET = 15
private const val HORIZONTAL_OFFSET = 8

class TagsPanel : NonOpaquePanel(HorizontalLayout(HORIZONTAL_OFFSET)), CourseSelectionListener {

  init {
    border = JBUI.Borders.empty(TAGS_TOP_OFFSET, HORIZONTAL_MARGIN, 0, 0)
  }

  private fun addTags(course: Course) {
    removeAll()
    val tags = course.tags
    isVisible = tags.isNotEmpty()
    for (tag in tags) {
      add(TagComponent(tag.text))
    }
  }

  fun update(course: Course, settings: CourseDisplaySettings) {
    isVisible = settings.showTagsPanel
    if (settings.showTagsPanel) {
      addTags(course)
    }
  }

  override fun onCourseSelectionChanged(data: CourseBindData) {
    val (course, courseDisplaySettings) = data
    isVisible = courseDisplaySettings.showTagsPanel
    if (courseDisplaySettings.showTagsPanel) {
      addTags(course)
    }
  }
}
