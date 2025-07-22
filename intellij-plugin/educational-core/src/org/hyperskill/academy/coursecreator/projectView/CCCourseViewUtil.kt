@file:JvmName("CCCourseViewUtil")

package org.hyperskill.academy.coursecreator.projectView

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.util.ui.tree.TreeUtil
import org.hyperskill.academy.coursecreator.framework.CCFrameworkLessonManager
import org.hyperskill.academy.coursecreator.framework.SyncChangesStateManager
import org.hyperskill.academy.coursecreator.framework.SyncChangesTaskFileState
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.getTaskFile
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.projectView.RootNode
import org.hyperskill.academy.learning.projectView.TaskNode
import javax.swing.tree.TreeNode


fun modifyNodeInEducatorMode(project: Project, viewSettings: ViewSettings, childNode: AbstractTreeNode<*>): AbstractTreeNode<*>? {
  val value = childNode.value
  return when (value) {
    is PsiDirectory -> CCNode(project, value, viewSettings, null)
    is PsiFile -> CCStudentInvisibleFileNode(project, value, viewSettings)
    else -> null
  }
}

fun findAncestorTaskNode(node: AbstractTreeNode<*>): TaskNode? {
  var currentNode = node
  while (currentNode !is TaskNode && currentNode !is RootNode) {
    currentNode = currentNode.parent
  }
  return currentNode as? TaskNode
}

fun isNodeInFrameworkLessonTask(node: PsiFileNode): Boolean {
  // find the task using parent nodes, because finding the task using VFS in mouse adapter will be slower
  val taskNode = findAncestorTaskNode(node)
  val task = taskNode?.item
  return task?.lesson is FrameworkLesson
}

fun SyncChangesHelpTooltip.tryInstallNewTooltip(project: Project, treeNode: TreeNode): Boolean {
  val node = TreeUtil.getUserObject(treeNode) as? CCFileNode ?: return false
  if (!isNodeInFrameworkLessonTask(node)) return false

  val taskFile = node.virtualFile?.getTaskFile(project) ?: return false

  var title: String?
  var description: String?
  var actionText: String?
  val state = SyncChangesStateManager.getInstance(project).getSyncChangesState(taskFile)

  when (state) {
    null -> return false
    SyncChangesTaskFileState.INFO -> {
      title = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.Changes.text")
      description = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.Changes.description")
      actionText = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ActionLink.Changes.text")
    }

    SyncChangesTaskFileState.WARNING -> {
      title = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.File.text")
      description = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ProjectView.Tooltip.File.description")
      actionText = EduCoreBundle.message("action.Educational.Educator.SyncChangesWithNextTasks.ActionLink.File.text")
    }
  }

  clearLinks()

  setTitle(title)
  setDescription(description)
  addLink(actionText) {
    CCFrameworkLessonManager.getInstance(project).propagateChanges(taskFile.task, listOf(taskFile))
  }
  setLocation(SyncChangesHelpTooltip.Alignment.EXACT_CURSOR)
  return true
}