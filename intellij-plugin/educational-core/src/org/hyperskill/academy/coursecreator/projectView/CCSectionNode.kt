package org.hyperskill.academy.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.projectView.LessonNode
import org.hyperskill.academy.learning.projectView.SectionNode

class CCSectionNode(
  project: Project,
  viewSettings: ViewSettings,
  section: Section,
  psiDirectory: PsiDirectory
) : SectionNode(project, viewSettings, section, psiDirectory) {

  override fun createLessonNode(directory: PsiDirectory, lesson: Lesson): LessonNode {
    return CCLessonNode(myProject, directory, settings, lesson)
  }

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    return node ?: modifyNodeInEducatorMode(myProject, settings, childNode)
  }
}
