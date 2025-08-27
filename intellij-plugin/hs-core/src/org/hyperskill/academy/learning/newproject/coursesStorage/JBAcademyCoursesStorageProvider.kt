package org.hyperskill.academy.learning.newproject.coursesStorage


import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CourseDataStorage
import com.intellij.openapi.wm.impl.welcomeScreen.learnIde.coursesInProgress.CoursesStorageProvider

class HyperskillAcademyCoursesStorageProvider : CoursesStorageProvider {
  override fun getCoursesStorage(): CourseDataStorage {
    return CoursesStorage.getInstance()
  }
}