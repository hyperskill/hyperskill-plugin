package org.hyperskill.academy.coursecreator.handlers.rename

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.rename.RenameDialog
import com.intellij.refactoring.rename.RenamePsiFileProcessor
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.EduUtilsKt.isStudentProject
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.DescriptionFormat
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.getContainingTask
import org.hyperskill.academy.learning.handlers.rename.EduRenameDialogBase
import org.hyperskill.academy.learning.handlers.rename.RenameDialogFactory
import org.hyperskill.academy.learning.handlers.rename.createRenameDialog
import org.hyperskill.academy.learning.messages.EduCoreBundle

class CCDescriptionFileRenameProcessor : RenamePsiFileProcessor() {

  override fun canProcessElement(element: PsiElement): Boolean {
    if (element !is PsiFile) return false
    val project = element.project
    if (project.isStudentProject()) return false
    val file = element.virtualFile
    if (!EduUtilsKt.isTaskDescriptionFile(file.name)) return false
    val task = file.getContainingTask(project) ?: return false
    return file.parent == task.getDir(project.courseDir)
  }

  override fun createRenameDialog(
    project: Project,
    element: PsiElement,
    nameSuggestionContext: PsiElement?,
    editor: Editor?
  ): RenameDialog {
    val task = (element as PsiFile).virtualFile.getContainingTask(project)
               ?: return super.createRenameDialog(project, element, nameSuggestionContext, editor)

    return createRenameDialog(project, element, nameSuggestionContext, editor, Factory(task))
  }

  private class Factory(private val task: Task) : RenameDialogFactory {
    override fun createRenameDialog(
      project: Project,
      element: PsiElement,
      nameSuggestionContext: PsiElement?,
      editor: Editor?
    ): EduRenameDialogBase {
      return object : EduRenameDialogBase(project, element, nameSuggestionContext, editor) {

        init {
          title = EduCoreBundle.message("dialog.title.rename.description.file")
        }

        override fun performRename(newName: String) {
          val format = DescriptionFormat.values().find { it.fileName == newName } ?: error("Unexpected new name: `$newName`")
          task.descriptionFormat = format
          super.performRename(newName)
        }

        @Throws(ConfigurationException::class)
        override fun canRun() {
          if (!EduUtilsKt.isTaskDescriptionFile(newName)) {
            throw ConfigurationException(
              EduCoreBundle.message(
                "dialog.message.incorrect.description.file.name",
                DescriptionFormat.HTML.fileName,
                DescriptionFormat.MD.fileName
              )
            )
          }
        }
      }
    }
  }
}
