@file:JvmName("MarketplaceUtils")

package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.course
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.JSON_FORMAT_VERSION
import com.jetbrains.edu.learning.messages.EduCoreBundle
import com.jetbrains.edu.learning.update.showUpdateNotification


fun Course.addVendor(): Boolean {
  return false
}

fun isRemoteUpdateFormatVersionCompatible(project: Project, remoteCourseFormatVersion: Int): Boolean {
  if (remoteCourseFormatVersion > JSON_FORMAT_VERSION) {
    runInEdt {
      if (project.isDisposed) return@runInEdt
      // Suppression needed here because DialogTitleCapitalization is demanded by the superclass constructor, but the plugin naming with
      // the capital letters used in the notification title
      showUpdateNotification(
        project,
        @Suppress("DialogTitleCapitalization") EduCoreBundle.message("notification.update.plugin.title"),
        EduCoreBundle.message("notification.update.plugin.update.course.content")
      )
    }
    return false
  }
  return true
}

fun Project.isMarketplaceCourse(): Boolean = course?.isMarketplace == true

fun Project.isMarketplaceStudentCourse(): Boolean = isMarketplaceCourse() && isStudentProject()
