package org.hyperskill.academy.learning.newproject.coursesStorage

import com.intellij.openapi.components.*
import org.hyperskill.academy.learning.EduTestAware
import org.hyperskill.academy.learning.newproject.ui.welcomeScreen.JBACourseFromStorage
import org.jetbrains.annotations.TestOnly

// It doesn't implement the platform's `CourseDataStorage`: nothing registers it as a `coursesStorageProvider` for the welcome screen Learn tab,
// and since 2027.1 that API lives in the separate `intellij.platform.ide.learnIde` module, so referencing it broke Browse Courses
@State(name = "HyperskillCoursesStorage", storages = [Storage("hsCoursesStorage.xml", roamingType = RoamingType.DISABLED)])
@Service
class CoursesStorage : CoursesStorageBase(), EduTestAware {

  fun removeCourseByLocation(location: String): Boolean {
    return super.doRemoveCourseByLocation(location)
  }

  fun getAllCourses(): List<JBACourseFromStorage> {
    return state.courses
  }

  @TestOnly
  override fun cleanUpState() {
    state.courses.clear()
  }

  companion object {
    fun getInstance(): CoursesStorage = service()
  }
}
