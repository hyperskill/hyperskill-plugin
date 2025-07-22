package org.hyperskill.academy.coursecreator.projectView

import com.intellij.psi.PsiDirectory
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.projectView.ContentHolderNode
import org.hyperskill.academy.learning.projectView.IntermediateDirectoryNode
import org.hyperskill.academy.learning.projectView.LessonNode
import org.hyperskill.academy.learning.projectView.SectionNode

interface CCContentHolderNode : ContentHolderNode {
  override fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode {
    return CCLessonNode(getProject(), directory, getSettings(), lesson)
  }

  override fun createSectionNode(directory: PsiDirectory, section: Section): SectionNode {
    return CCSectionNode(getProject(), getSettings(), section, directory)
  }

  override fun createIntermediateDirectoryNode(directory: PsiDirectory, course: Course): IntermediateDirectoryNode {
    return CCIntermediateDirectoryNode(getProject(), course, directory, getSettings())
  }
}