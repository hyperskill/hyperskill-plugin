package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.ext.sourceDir
import org.hyperskill.academy.learning.courseFormat.ext.testDirs
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.projectView.CourseViewUtils.modifyTaskChildNode

open class DirectoryNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  task: Task?
) : EduNode<Task>(project, value, viewSettings, task) {

  /** The task is only kept to decide which children are visible; the node itself stands for the directory. */
  override val presentedItem: Task?
    get() = null

  override val presentableName: String
    get() {
      val name = value.virtualFile.name
      val course = StudyTaskManager.getInstance(myProject).course ?: return super.presentableName
      // A source or test directory always keeps its own name, whatever the project view would shorten it to
      return if (name == course.sourceDir || name in course.testDirs) name else super.presentableName
    }

  override fun canNavigate(): Boolean = true

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
