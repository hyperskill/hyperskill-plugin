package org.hyperskill.academy.coursecreator.handlers.move

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.refactoring.move.MoveCallback
import org.hyperskill.academy.coursecreator.CCUtils.updateHigherElements
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.isSectionDirectory
import org.hyperskill.academy.learning.messages.EduCoreBundle.message
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.saveItem

class CCSectionMoveHandlerDelegate : CCStudyItemMoveHandlerDelegate(StudyItemType.SECTION_TYPE) {
  override fun isAvailable(directory: PsiDirectory): Boolean {
    return directory.virtualFile.isSectionDirectory(directory.project)
  }

  override fun doMove(
    project: Project,
    elements: Array<PsiElement>,
    targetDirectory: PsiElement?,
    callback: MoveCallback?
  ) {
    if (targetDirectory !is PsiDirectory) {
      return
    }
    val course = StudyTaskManager.getInstance(project).course ?: return
    val sourceDirectory = elements[0] as PsiDirectory
    val sourceSection = course.getSection(sourceDirectory.name)
                        ?: throw IllegalStateException("Failed to find section for `sourceVFile` directory")
    val targetItem = course.getItem(targetDirectory.name)
    if (targetItem == null) {
      Messages.showInfoMessage(
        message("dialog.message.incorrect.movement.section"),
        message("dialog.title.incorrect.target.for.move")
      )
      return
    }
    val delta = getDelta(project, targetItem) ?: return
    val sourceSectionIndex = sourceSection.index
    sourceSection.index = -1
    val itemDirs = project.courseDir.children
    updateHigherElements(itemDirs, { file: VirtualFile -> course.getItem(file.name) }, sourceSectionIndex, -1)
    val newItemIndex = targetItem.index + delta
    updateHigherElements(itemDirs, { file: VirtualFile -> course.getItem(file.name) }, newItemIndex - 1, 1)
    sourceSection.index = newItemIndex
    course.sortItems()
    ProjectView.getInstance(project).refresh()
    saveItem(course)
  }
}
