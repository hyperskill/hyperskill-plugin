package org.hyperskill.academy.jvm.gradle

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.RefreshCause

interface GradleCourseRefresher {
  fun isAvailable(): Boolean
  fun refresh(project: Project, cause: RefreshCause)

  companion object {
    val EP_NAME: ExtensionPointName<GradleCourseRefresher> = ExtensionPointName.create("HyperskillEducational.gradleRefresher")

    fun firstAvailable(): GradleCourseRefresher? = EP_NAME.extensionList.firstOrNull(GradleCourseRefresher::isAvailable)
  }
}
