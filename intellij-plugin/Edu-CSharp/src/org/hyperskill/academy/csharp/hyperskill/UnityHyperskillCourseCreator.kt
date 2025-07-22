package org.hyperskill.academy.csharp.hyperskill

import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillCourseCreator

class UnityHyperskillCourseCreator : HyperskillCourseCreator {
  override fun createHyperskillCourse(
    hyperskillProject: HyperskillProject,
    languageId: String,
    languageVersion: String?,
    eduEnvironment: String
  ): HyperskillCourse {
    val customContentPath = if (hyperskillProject.language == "unity") "Assets/Scripts/Editor" else ""
    return HyperskillCourse(hyperskillProject, languageId, languageVersion, eduEnvironment).apply {
      this.customContentPath = customContentPath
    }
  }
}