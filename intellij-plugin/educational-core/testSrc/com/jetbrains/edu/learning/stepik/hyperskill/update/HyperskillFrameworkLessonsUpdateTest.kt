package org.hyperskill.academy.learning.stepik.hyperskill.update

import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.update.CourseUpdater
import org.hyperskill.academy.learning.update.FrameworkLessonsUpdateTest

class HyperskillFrameworkLessonsUpdateTest : FrameworkLessonsUpdateTest<HyperskillCourse>() {

  override fun produceCourse(): HyperskillCourse = HyperskillCourse()

  override fun setupLocalCourse(course: HyperskillCourse) {
    course.hyperskillProject = HyperskillProject().apply {
      title = course.name
      description = course.description
    }
    course.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2), HyperskillStage(3, "", 3))
  }

  override fun getUpdater(localCourse: HyperskillCourse): CourseUpdater<HyperskillCourse> = HyperskillCourseUpdaterNew(project, localCourse)
}
