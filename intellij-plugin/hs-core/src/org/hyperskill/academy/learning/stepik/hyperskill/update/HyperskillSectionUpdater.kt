package org.hyperskill.academy.learning.stepik.hyperskill.update

import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.update.HyperskillItemUpdater
import org.hyperskill.academy.learning.update.LessonUpdater
import org.hyperskill.academy.learning.update.SectionUpdater

class HyperskillSectionUpdater(project: Project, course: Course) : SectionUpdater(project, course), HyperskillItemUpdater<Section> {
  override fun createLessonUpdater(section: Section): LessonUpdater = HyperskillLessonUpdater(project, section)
}