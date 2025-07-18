package com.jetbrains.edu.rust.learn

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.edu.learning.newproject.coursesStorage.CoursesStorage
import com.jetbrains.edu.learning.notification.EduNotificationManager
import com.jetbrains.edu.rust.messages.EduRustBundle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.rust.cargo.toolchain.RsToolchainBase
import java.nio.file.Path

// BACKCOMPAT: 2025.1. Merge it into RsOpenCourseHandler
object RsOpenCourseHelper {

  private const val RUST_ROVER_BANNER = "rustrover_banner"

  private val LOG = logger<RsOpenCourseHelper>()

  suspend fun openCourse(
    courseId: Int,
    toolchain: RsToolchainBase,
    projectLocation: Path?
  ) {
    val location = searchExistingCourseLocation(courseId)
    if (location != null) {
      withContext(Dispatchers.EDT) {
        val project = ProjectUtil.openProject(location, null, true)
        ProjectUtil.focusProjectWindow(project, true)
      }
    }
    else {
      LOG.warn("Failed to load https://plugins.jetbrains.com/plugin/$courseId course")
      EduNotificationManager.showErrorNotification(
        title = EduRustBundle.message("course.creation.failed.notification.title"),
        content = EduRustBundle.message("course.creation.no.course.found.notification.content")
      )
    }
  }

  fun isAlreadyStartedCourse(courseId: Int): Boolean {
    return searchExistingCourseLocation(courseId) != null
  }

  private fun searchExistingCourseLocation(courseId: Int): String? {
    val location = CoursesStorage.getInstance().getAllCourses().find { it.id == courseId }?.location ?: return null
    return if (FileUtil.exists(location)) location else null
  }
}
