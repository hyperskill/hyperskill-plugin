package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
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
    val task = item.getTask(directory.name)
    if (task == null) {
      // The directory is dropped from the tree, so a task shown under its own directory name is the only visible
      // sign that this happened. Naming the tasks the lesson does know about tells apart "the model lost the task"
      // from "the platform handed us a directory that is not a task directory at all".
      LOG.warn(
        "No task named `${directory.name}` in lesson `${item.name}` (${directory.virtualFile.path})." +
        " Known tasks: [${item.taskList.joinToString { it.name }}]"
      )
      return null
    }
    val taskDirectory = findTaskDirectory(myProject, directory, task) ?: return null
    return createTaskNode(taskDirectory, task)
  }

  protected open fun createTaskNode(directory: PsiDirectory, task: Task): TaskNode {
    return TaskNode(myProject, directory, settings, task)
  }

  override val item: Lesson get() = super.item!!

  companion object {
    private val LOG: Logger = Logger.getInstance(LessonNode::class.java)
  }
}
