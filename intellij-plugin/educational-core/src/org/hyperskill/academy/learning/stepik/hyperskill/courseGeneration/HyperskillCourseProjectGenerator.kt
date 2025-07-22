package org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.CourseInfoHolder
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.learning.newproject.EduProjectSettings

open class HyperskillCourseProjectGenerator<T : EduProjectSettings>(
  private val base: CourseProjectGenerator<T>,
  builder: HyperskillCourseBuilder<T>,
  course: HyperskillCourse
) : CourseProjectGenerator<T>(builder, course) {

  override fun afterProjectGenerated(
    project: Project,
    projectSettings: T,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) =
    base.afterProjectGenerated(project, projectSettings, openCourseParams, onConfigurationFinished)

  override fun createAdditionalFiles(holder: CourseInfoHolder<Course>) =
    base.createAdditionalFiles(holder)

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> =
    base.autoCreatedAdditionalFiles(holder)

  override suspend fun createCourseStructure(holder: CourseInfoHolder<Course>, initialLessonProducer: () -> Lesson) =
    base.createCourseStructure(holder, initialLessonProducer)
}
