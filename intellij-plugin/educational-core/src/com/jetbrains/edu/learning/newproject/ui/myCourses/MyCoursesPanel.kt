package com.jetbrains.edu.learning.newproject.ui.myCourses

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBPanelWithEmptyText
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.newproject.ui.CourseCardComponent
import com.jetbrains.edu.learning.newproject.ui.CoursesPanel
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.ToolbarActionWrapper
import com.jetbrains.edu.learning.newproject.ui.coursePanel.groups.CoursesGroup
import com.jetbrains.edu.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import kotlinx.coroutines.CoroutineScope

class MyCoursesPanel(
  myCoursesProvider: CoursesPlatformProvider,
  scope: CoroutineScope,
  disposable: Disposable
) : CoursesPanel(myCoursesProvider, scope, disposable) {

  override fun toolbarAction(): ToolbarActionWrapper? {
    coursesSearchComponent.hideFilters()
    return null
  }

  override fun setNoCoursesPanelDefaultText(panel: JBPanelWithEmptyText) {
    val emptyText = panel.emptyText
    emptyText.text = EduCoreBundle.message("course.dialog.my.courses.no.courses.started")
  }

  override fun updateFilters(coursesGroups: List<CoursesGroup>) {
    super.updateFilters(coursesGroups)
    coursesSearchComponent.selectAllHumanLanguageItems()
  }

  override fun updateModelAfterCourseDeletedFromStorage(deletedCourse: JBACourseFromStorage) {
    coursesGroups.clear()
    coursesGroups.addAll(CoursesStorage.getInstance().coursesInGroups())
    super.updateModelAfterCourseDeletedFromStorage(deletedCourse)
  }

  override fun createCoursesListPanel() = MyCoursesList()

  inner class MyCoursesList : CoursesListWithResetFilters() {
    override fun createCourseCard(course: Course): CourseCardComponent {
      return MyCourseCardComponent(course)
    }
  }
}
