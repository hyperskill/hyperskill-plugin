package org.hyperskill.academy.learning.update

import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings

abstract class UpdatesAvailableTestBase<T : Course> : CourseGenerationTestBase<EmptyProjectSettings>() {
  protected lateinit var localCourse: T

  override val defaultSettings: EmptyProjectSettings get() = EmptyProjectSettings

  abstract fun initiateLocalCourse()

  override fun runInDispatchThread(): Boolean = false
}