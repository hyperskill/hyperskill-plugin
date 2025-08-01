package org.hyperskill.academy.learning.newproject.ui.coursePanel.groups

import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.newproject.ui.CourseCardComponent
import org.hyperskill.academy.learning.newproject.ui.coursePanel.SelectCourseBackgroundColor
import org.hyperskill.academy.learning.newproject.ui.myCourses.MyCourseCardComponent
import java.awt.BorderLayout
import javax.swing.JPanel


abstract class CoursesListPanel : JPanel(BorderLayout()) {
  private val groupsComponent: GroupsComponent = GroupsComponent(::createCourseCard, ::resetFilters)
  val selectedCourse: Course? get() = groupsComponent.selectedValue

  init {
    background = SelectCourseBackgroundColor
    this.add(groupsComponent, BorderLayout.CENTER)
  }

  protected open fun createCourseCard(course: Course): CourseCardComponent {
    val courseMetaInfo = CoursesStorage.getInstance().getCourseMetaInfo(course)
    return if (courseMetaInfo != null) {
      MyCourseCardComponent(course)
    }
    else {
      createCardForNewCourse(course)
    }
  }

  protected open fun createCardForNewCourse(course: Course): CourseCardComponent = CourseCardComponent(course)

  protected abstract fun resetFilters()

  fun updateModel(coursesGroups: List<CoursesGroup>, courseToSelect: Course?) {
    clear()

    if (coursesGroups.isEmpty()) {
      return
    }

    coursesGroups.forEach { coursesGroup ->
      if (coursesGroup.courses.isNotEmpty()) {
        addGroup(coursesGroup)
      }
    }

    if (courseToSelect == null) {
      initialSelection()
      return
    }

    val newCourseToSelect = coursesGroups.flatMap { it.courses }.firstOrNull { course: Course -> course == courseToSelect }
    setSelectedValue(newCourseToSelect)
  }

  private fun addGroup(coursesGroup: CoursesGroup) {
    groupsComponent.addGroup(coursesGroup)
  }

  fun clear() {
    groupsComponent.clear()
  }

  fun setSelectedValue(newCourseToSelect: Course?) {
    groupsComponent.setSelectedValue(newCourseToSelect)
  }

  private fun initialSelection() {
    groupsComponent.initialSelection()
  }

  fun setSelectionListener(processSelectionChanged: () -> Unit) {
    groupsComponent.setSelectionListener(processSelectionChanged)
  }

}