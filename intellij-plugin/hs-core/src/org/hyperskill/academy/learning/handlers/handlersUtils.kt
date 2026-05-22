@file:JvmName("HandlersUtils")

package org.hyperskill.academy.learning.handlers

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.hyperskill.academy.learning.*

private fun isStudyItemDirectory(project: Project, element: PsiElement): Boolean {
  val dir = (element as? PsiDirectory)?.virtualFile ?: return false
  return dir.getStudyItem(project) != null
}

private fun isTaskDescriptionFile(project: Project, element: PsiElement): Boolean {
  val file = (element as? PsiFile)?.originalFile?.virtualFile ?: return false
  return EduUtilsKt.isTaskDescriptionFile(file.name) && file.parent == file.getTaskDir(project)
}

private fun isTaskFile(project: Project, element: PsiElement): Boolean {
  val file = (element as? PsiFile)?.originalFile?.virtualFile ?: return false
  return file.getTaskFile(project) != null
}

private fun isCourseAdditionalFile(project: Project, element: PsiElement): Boolean {
  val file = (element as? PsiFile)?.originalFile?.virtualFile ?: return false
  val course = project.course ?: return false
  val path = FileUtil.getRelativePath(project.courseDir.path, file.path, '/') ?: return false
  return course.additionalFiles.any { it.name == path }
}

private fun isRenameRefactoringForbidden(project: Project?, element: PsiElement?): Boolean {
  if (project == null || element == null) return false

  return when (element) {
    is PsiFile -> isTaskFile(project, element) || isTaskDescriptionFile(project, element) || isCourseAdditionalFile(project, element)
    is PsiDirectory -> isStudyItemDirectory(project, element)
    else -> false
  }
}

fun isRenameForbidden(project: Project?, element: PsiElement?): Boolean {
  return isRenameRefactoringForbidden(project, element)
}

fun isMoveForbidden(project: Project?, element: PsiElement?, target: PsiElement?): Boolean {
  if (project?.course == null) return false
  if (element == null) return false
  if (isStudyItemDirectory(project, element) || isTaskDescriptionFile(project, element) || isCourseAdditionalFile(project, element)) {
    return true
  }
  if (element is PsiFile) {
    try {
      val targetDir = (target as? PsiDirectory)?.virtualFile ?: return false
      val targetTaskDir = if (targetDir.isTaskDirectory(project)) {
        targetDir
      }
      else {
        targetDir.getTaskDir(project)
      }
      val sourceTaskDir = element.originalFile.virtualFile.getTaskDir(project) ?: return false

      if (sourceTaskDir != targetTaskDir) return true
    }
    catch (e: Exception) {
      // If we get an exception when trying to get the task directory,
      // it's likely because the file belongs to a different project.
      // In this case, we should forbid the move operation.
      return true
    }
  }
  return false
}

fun isMoveForbidden(dataContext: DataContext): Boolean = isMoveForbidden(
  CommonDataKeys.PROJECT.getData(dataContext),
  CommonDataKeys.PSI_ELEMENT.getData(dataContext),
  LangDataKeys.TARGET_PSI_ELEMENT.getData(dataContext)
)
