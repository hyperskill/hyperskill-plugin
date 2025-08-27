package org.hyperskill.academy.learning.courseFormat

enum class CourseMode {
  STUDENT;

  /**
   * String constants are different from variable names to provide backward compatibility with old courses
   */
  override fun toString(): String = when (this) {
    STUDENT -> STUDY
  }

  companion object {
    private const val STUDY = "Study"

    fun String.toCourseMode(): CourseMode? = when (this) {
      STUDY -> STUDENT
      else -> null
    }
  }
}