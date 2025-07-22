package org.hyperskill.academy.learning.update

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.LessonContainer
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.update.comparators.EduFileComparator.Companion.areNotEqual
import org.hyperskill.academy.learning.update.elements.CourseUpdate
import org.hyperskill.academy.learning.update.elements.StudyItemUpdate

abstract class CourseUpdater<T : Course>(val project: Project, private val localCourse: T) : ItemUpdater<T> {
  protected abstract fun createLessonUpdater(container: LessonContainer): LessonUpdater
  protected abstract fun createSectionUpdater(course: T): SectionUpdater

  suspend fun collect(remoteCourse: T): Collection<StudyItemUpdate<StudyItem>> {
    val updates = mutableListOf<StudyItemUpdate<StudyItem>>()

    val sectionUpdater = createSectionUpdater(localCourse)
    val sectionUpdates = sectionUpdater.collect(remoteCourse)
    updates.addAll(sectionUpdates)

    val lessonUpdater = createLessonUpdater(localCourse)
    val lessonUpdates = lessonUpdater.collect(remoteCourse)
    updates.addAll(lessonUpdates)

    if (updates.isNotEmpty() || localCourse.isOutdated(remoteCourse) || isCourseChanged(localCourse, remoteCourse)) {
      // If any changes are detected in the course items, keep in mind that sorting may be required
      updates.add(CourseUpdate.get(localCourse, remoteCourse))
    }

    return updates
  }

  suspend fun update(remoteCourse: T) {
    val updates = collect(remoteCourse)
    updates.forEach {
      it.update(project)
    }
  }

  abstract fun isCourseChanged(localCourse: T, remoteCourse: T): Boolean

  protected fun T.isChanged(remoteCourse: T): Boolean =
    when {
      name != remoteCourse.name -> true
      additionalFiles areNotEqual remoteCourse.additionalFiles -> true
      else -> false
    }
}