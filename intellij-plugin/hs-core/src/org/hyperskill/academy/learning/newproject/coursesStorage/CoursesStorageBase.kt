package org.hyperskill.academy.learning.newproject.coursesStorage

import com.intellij.ide.RecentProjectsManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.components.SimplePersistentStateComponent
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.util.SlowOperations
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.annotations.XCollection
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.ui.coursePanel.groups.CoursesGroup
import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage


open class CoursesStorageBase : SimplePersistentStateComponent<UserCoursesState>(UserCoursesState()) {

  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    state.addCourse(course, location, tasksSolved, tasksTotal)
    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_ADDED).courseAdded(course)
  }

  open fun getCoursePath(course: Course): String? = getCourseMetaInfo(course)?.location

  fun hasCourse(course: Course): Boolean {
    val courseMetaInfo = getCourseMetaInfo(course) ?: return false
    // Allow slow VFS operations when checking if course location is valid
    return SlowOperations.allowSlowOperations<Boolean> {
      courseMetaInfo.isLocationValid
    }
  }

  protected fun doRemoveCourseByLocation(location: String): Boolean {
    val deletedCourse = state.removeCourseByLocation(location) ?: return false
    ApplicationManager.getApplication().messageBus.syncPublisher(COURSE_DELETED).courseDeleted(deletedCourse)
    RecentProjectsManager.getInstance().removePath(location)

    return true
  }

  fun getCourseMetaInfo(course: Course): JBACourseFromStorage? {
    return state.courses.find {
      it.name == course.name
      && it.id == course.id
      && it.courseMode == course.courseMode
      && it.languageId == course.languageId
    }
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    state.updateCourseProgress(course, location, tasksSolved, tasksTotal)
  }

  /**
   * Updates the file path for a course in storage.
   * Used when user relocates course files to a different directory.
   */
  fun updateCoursePath(course: Course, newLocation: String) {
    val courseMetaInfo = getCourseMetaInfo(course) ?: return
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(newLocation)
    courseMetaInfo.location = systemIndependentLocation
    // Trigger state modification by updating an existing field
    state.updateCoursePath(courseMetaInfo, systemIndependentLocation)
  }

  fun coursesInGroups(): List<CoursesGroup> {
    val courses = state.courses.filter { it.isLocationValid }
    val solvedCourses = courses.filter { it.isStudy && it.tasksSolved != 0 && it.tasksSolved == it.tasksTotal }.map { it.toCourse() }
    val solvedCoursesGroup = CoursesGroup(EduCoreBundle.message("course.dialog.completed"), solvedCourses)

    val courseCreatorCoursesGroup = CoursesGroup(
      EduCoreBundle.message("course.dialog.my.courses.course.creation"),
      courses.filter { !it.isStudy }.map { it.toCourse() }
    )

    val inProgressCourses = courses.filter { it.isStudy && (it.tasksSolved == 0 || it.tasksSolved != it.tasksTotal) }.map { it.toCourse() }
    val inProgressCoursesGroup = CoursesGroup(EduCoreBundle.message("course.dialog.in.progress"), inProgressCourses)

    return listOf(courseCreatorCoursesGroup, inProgressCoursesGroup, solvedCoursesGroup).filter { it.courses.isNotEmpty() }
  }

  fun isNotEmpty() = state.courses.isNotEmpty()

  companion object {
    val COURSE_DELETED = Topic.create("Hyperskill.courseDeletedFromStorage", CourseDeletedListener::class.java)
    val COURSE_ADDED = Topic.create("Hyperskill.courseAddedToStorage", CourseAddedListener::class.java)
  }
}

class UserCoursesState : BaseState() {
  //  courses list is not updated on course removal and could contain removed courses.
  @get:XCollection(style = XCollection.Style.v2)
  val courses by list<JBACourseFromStorage>()

  fun addCourse(course: Course, location: String, tasksSolved: Int = 0, tasksTotal: Int = 0) {
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(location)
    courses.removeIf { it.location == systemIndependentLocation }
    val courseMetaInfo = JBACourseFromStorage(systemIndependentLocation, course, tasksTotal, tasksSolved)
    courses.add(courseMetaInfo)
  }

  fun removeCourseByLocation(location: String): JBACourseFromStorage? {
    val courseMetaInfo = courses.find { it.location == location }
    courses.remove(courseMetaInfo)
    return courseMetaInfo
  }

  fun updateCourseProgress(course: Course, location: String, tasksSolved: Int, tasksTotal: Int) {
    val systemIndependentLocation = FileUtilRt.toSystemIndependentName(location)
    val courseMetaInfo = courses.find { it.location == systemIndependentLocation }
    if (courseMetaInfo != null) {
      courseMetaInfo.tasksSolved = tasksSolved
      courseMetaInfo.tasksTotal = tasksTotal
      incrementModificationCount()
    }
    else {
      courses.add(JBACourseFromStorage(systemIndependentLocation, course, tasksTotal, tasksSolved))
    }
  }

  fun updateCoursePath(courseMetaInfo: JBACourseFromStorage, newLocation: String) {
    courseMetaInfo.location = newLocation
    incrementModificationCount()
  }
}