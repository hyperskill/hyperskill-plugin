package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ProjectViewDirectoryHelper
import com.intellij.ide.projectView.impl.nodes.ProjectViewProjectNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.isUnitTestMode

class RootNode(project: Project, viewSettings: ViewSettings?) : ProjectViewProjectNode(project, viewSettings) {

  override fun getChildren(): Collection<AbstractTreeNode<*>> {
    val course = StudyTaskManager.getInstance(myProject).course ?: return emptyList()
    val nodes = ArrayList<AbstractTreeNode<*>>()
    if (!isUnitTestMode) {
      val psiDirectory = PsiManager.getInstance(myProject).findDirectory(myProject.courseDir)
      addCourseNode(course, nodes, psiDirectory)
    }
    else {
      val topLevelContentRoots = ProjectViewDirectoryHelper.getInstance(myProject).topLevelRoots
      for (root in topLevelContentRoots) {
        val psiDirectory = PsiManager.getInstance(myProject).findDirectory(root)
        addCourseNode(course, nodes, psiDirectory)
      }
    }
    return nodes
  }

  private fun addCourseNode(course: Course, nodes: MutableList<AbstractTreeNode<*>>, psiDirectory: PsiDirectory?) {
    if (psiDirectory == null) return
    nodes += CourseNode(myProject, psiDirectory, settings, course)
  }
}
