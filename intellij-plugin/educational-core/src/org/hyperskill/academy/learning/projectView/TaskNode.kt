package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.navigation.NavigationUtils.navigateToTask
import org.hyperskill.academy.learning.projectView.CourseViewUtils.modifyTaskChildNode

open class TaskNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  task: Task
) : EduNode<Task>(project, value, viewSettings, task) {

  override val item: Task get() = super.item!!

  override fun getWeight(): Int = item.index
  override fun expandOnDoubleClick(): Boolean = false
  override fun canNavigate(): Boolean = true

  override fun navigate(requestFocus: Boolean) {
    navigateToTask(myProject, item)
  }

  public override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    return modifyTaskChildNode(myProject, childNode, item, this::createChildFileNode, this::createChildDirectoryNode)
  }

  open fun createChildDirectoryNode(value: PsiDirectory): PsiDirectoryNode {
    return DirectoryNode(myProject, value, settings, item)
  }

  open fun createChildFileNode(originalNode: AbstractTreeNode<*>, psiFile: PsiFile): AbstractTreeNode<*> {
    return originalNode
  }
}
