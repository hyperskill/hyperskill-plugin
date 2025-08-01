package org.hyperskill.academy.learning.newproject.ui

import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.panels.NonOpaquePanel
import com.intellij.util.ui.JBUI
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.ui.coursePanel.CoursePanel
import org.hyperskill.academy.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import org.hyperskill.academy.learning.newproject.ui.coursePanel.groups.CoursesGroup
import org.hyperskill.academy.learning.newproject.ui.coursePanel.groups.CoursesListPanel
import java.awt.BorderLayout
import javax.swing.ScrollPaneConstants

private const val TOOLBAR_TOP_OFFSET = 10
private const val TOOLBAR_BOTTOM_OFFSET = 8
private const val TOOLBAR_LEFT_OFFSET = 13

class CoursesListDecorator(
  private val mainPanel: CoursesListPanel,
  tabDescription: String?,
  toolbarAction: ToolbarActionWrapper?
) : NonOpaquePanel() {
  private var myTabDescriptionPanel: TabDescriptionPanel? = null

  init {
    val listWithTabInfo = NonOpaquePanel()
    listWithTabInfo.add(mainPanel, BorderLayout.CENTER)

    if (tabDescription != null) {
      myTabDescriptionPanel = TabDescriptionPanel(tabDescription).apply {
        listWithTabInfo.add(this, BorderLayout.NORTH)
      }
    }

    val scrollPane = JBScrollPane(
      listWithTabInfo,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    )
    scrollPane.apply {
      border = JBUI.Borders.empty()
      background = SelectCourseBackgroundColor
    }

    add(scrollPane, BorderLayout.CENTER)

    if (toolbarAction != null) {
      val toolbarPanel = createHyperlinkWithContextHelp(toolbarAction).apply {
        border = JBUI.Borders.empty(TOOLBAR_TOP_OFFSET, TOOLBAR_LEFT_OFFSET, TOOLBAR_BOTTOM_OFFSET, 0)
      }
      add(toolbarPanel, BorderLayout.SOUTH)
      scrollPane.border = JBUI.Borders.customLine(CoursePanel.DIVIDER_COLOR, 0, 0, 1, 0)
    }
  }

  fun setSelectionListener(processSelectionChanged: () -> Unit) {
    mainPanel.setSelectionListener(processSelectionChanged)
  }

  fun updateModel(coursesGroups: List<CoursesGroup>, courseToSelect: Course?) {
    mainPanel.updateModel(coursesGroups, courseToSelect)
  }

  fun setSelectedValue(newCourseToSelect: Course?) {
    mainPanel.setSelectedValue(newCourseToSelect)
  }

  fun getSelectedCourse() = mainPanel.selectedCourse
}