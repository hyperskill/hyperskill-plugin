package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.projectView.FrameworkLessonNode.Companion.createFrameworkLessonNode

/**
 * A node whose descendants contain course content -- sections or lessons.
 * Primarily used to show the folders of [Course.customContentPath] to the student.
 * In other words, a node is a `[ContentHolderNode]` if any of the following conditions is met:
 * - The node is a Course
 * - The node is a part of [Course.customContentPath]
 */
interface ContentHolderNode {
  fun getProject(): Project
  fun getSettings(): ViewSettings

  fun createSectionNode(directory: PsiDirectory, section: Section): SectionNode {
    return SectionNode(getProject(), getSettings(), section, directory)
  }

  fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode? = if (lesson is FrameworkLesson) {
    createFrameworkLessonNode(getProject(), directory, getSettings(), lesson)
  }
  else {
    LessonNode(getProject(), directory, getSettings(), lesson)
  }

  fun createIntermediateDirectoryNode(directory: PsiDirectory, course: Course): IntermediateDirectoryNode {
    return IntermediateDirectoryNode(getProject(), directory, getSettings(), course)
  }
}