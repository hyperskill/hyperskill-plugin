package org.hyperskill.academy.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.projectView.LessonNode
import org.hyperskill.academy.learning.projectView.TaskNode

class CCLessonNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  lesson: Lesson
) : LessonNode(project, value, viewSettings, lesson) {
  public override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val node = super.modifyChildNode(childNode)
    return node ?: modifyNodeInEducatorMode(myProject, settings, childNode)
  }

  override fun createTaskNode(directory: PsiDirectory, task: Task): TaskNode {
    return CCTaskNode(myProject, directory, settings, task)
  }
}
