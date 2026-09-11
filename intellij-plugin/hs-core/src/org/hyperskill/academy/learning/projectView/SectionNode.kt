package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.hyperskill.academy.learning.courseFormat.Section

open class SectionNode(
  project: Project,
  viewSettings: ViewSettings,
  section: Section,
  psiDirectory: PsiDirectory
) : ContentHolderNode, EduNode<Section>(project, psiDirectory, viewSettings, section) {

  override val item: Section get() = super.item!!

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val lesson = item.getLesson(directory.name) ?: return null
    return createLessonNode(directory, lesson)
  }

  override fun getWeight(): Int = item.index
}
