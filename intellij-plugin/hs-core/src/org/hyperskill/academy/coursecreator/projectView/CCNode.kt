package org.hyperskill.academy.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.isTestsFile
import org.hyperskill.academy.learning.projectView.DirectoryNode

class CCNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  task: Task?
) : DirectoryNode(project, value, viewSettings, task) {

  override fun canNavigate(): Boolean = true

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    if (node != null) return node
    val value = childNode.value

    if (value is PsiDirectory) {
      return CCNode(myProject, value, settings, item)
    }
    if (value is PsiElement) {
      val psiFile = value.containingFile
      val virtualFile = psiFile.virtualFile
      val course = StudyTaskManager.getInstance(myProject).course ?: return null
      if (course.configurator == null) return CCStudentInvisibleFileNode(myProject, psiFile, settings)
      if (EduUtilsKt.isTaskDescriptionFile(virtualFile.name)) {
        return null
      }
      if (!virtualFile.isTestsFile(myProject)) {
        return CCStudentInvisibleFileNode(myProject, psiFile, settings)
      }
    }
    return null
  }

  override fun createChildDirectoryNode(value: PsiDirectory): PsiDirectoryNode {
    return CCNode(myProject, value, settings, item)
  }

  override fun createChildFileNode(originalNode: AbstractTreeNode<*>, psiFile: PsiFile): PsiFileNode {
    return CCFileNode(myProject, psiFile, settings)
  }
}
