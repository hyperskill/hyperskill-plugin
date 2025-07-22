package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.projectView.CourseViewUtils.findTaskDirectory

open class LessonNode(
  project: Project,
  value: PsiDirectory,
  viewSettings: ViewSettings,
  lesson: Lesson
) : EduNode<Lesson>(project, value, viewSettings, lesson) {

  override fun getWeight(): Int = item.index

  override fun modifyChildNode(childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
    val directory = childNode.value as? PsiDirectory ?: return null
    val task = item.getTask(directory.name) ?: return null
    val taskDirectory = findTaskDirectory(myProject, directory, task) ?: return null
    return createTaskNode(taskDirectory, task)
  }

  protected open fun createTaskNode(directory: PsiDirectory, task: Task): TaskNode {
    return TaskNode(myProject, directory, settings, task)
  }

  override val item: Lesson get() = super.item!!
}
