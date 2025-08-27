package org.hyperskill.academy.learning.newproject.ui.myCourses

import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBPanelWithEmptyText
import kotlinx.coroutines.CoroutineScope
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.newproject.ui.CourseCardComponent
import org.hyperskill.academy.learning.newproject.ui.CoursesPanel
import org.hyperskill.academy.learning.newproject.ui.CoursesPlatformProvider
import org.hyperskill.academy.learning.newproject.ui.ToolbarActionWrapper
import org.hyperskill.academy.learning.newproject.ui.coursePanel.groups.CoursesGroup
import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage

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
