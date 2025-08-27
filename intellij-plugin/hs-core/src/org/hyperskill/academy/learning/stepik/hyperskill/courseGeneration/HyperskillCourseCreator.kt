package org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.extensions.ExtensionPointName
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject

/**
 * Extension point to customize the creation of HyperskillCourses
 * Initially created to set the [org.hyperskill.academy.learning.courseFormat.Course.customContentPath] property
 */
interface HyperskillCourseCreator {
  fun createHyperskillCourse(
    hyperskillProject: HyperskillProject,
    languageId: String,
    languageVersion: String?,
    eduEnvironment: String
  ): HyperskillCourse

  companion object {
    val EP_NAME = ExtensionPointName.create<HyperskillCourseCreator>("HyperskillEducational.hyperskillCourseCreator")

    fun createHyperskillCourse(
      hyperskillProject: HyperskillProject,
      languageId: String,
      languageVersion: String?,
      eduEnvironment: String
    ): HyperskillCourse {
      val course = EP_NAME.computeSafeIfAny { it.createHyperskillCourse(hyperskillProject, languageId, languageVersion, eduEnvironment) }
      if (course != null) return course
      return HyperskillCourse(
        hyperskillProject, languageId, languageVersion, eduEnvironment
      )
    }
  }
}