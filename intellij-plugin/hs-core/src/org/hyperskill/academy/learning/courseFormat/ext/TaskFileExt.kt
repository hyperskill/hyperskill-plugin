@file:JvmName("TaskFileExt")

package org.hyperskill.academy.learning.courseFormat.ext

import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.problems.WolfTheProblemSolver
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.EduFile.Companion.LOG
import org.hyperskill.academy.learning.courseFormat.EduFileErrorHighlightLevel
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer


fun TaskFile.getDocument(project: Project): Document? {
  val virtualFile = getVirtualFile(project) ?: return null
  return runReadAction { FileDocumentManager.getInstance().getDocument(virtualFile) }
}

fun TaskFile.getVirtualFile(project: Project): VirtualFile? {
  val taskDir = task.getDir(project.courseDir) ?: return null
  return this.findTaskFileInDir(taskDir)
}

fun TaskFile.findTaskFileInDir(taskDir: VirtualFile): VirtualFile? {
  return runReadAction { taskDir.findFileByRelativePath(name) }
}

fun TaskFile.course() = task.lesson.course

fun TaskFile.getText(project: Project): String? = getDocument(project)?.text

val TaskFile.isTestFile: Boolean
  get() {
    val configurator = task.course.configurator ?: return false
    return configurator.isTestFile(task, name)
  }


fun TaskFile.revert(project: Project) {
  if (!resetDocument(project)) {
    return
  }

  val virtualFile = getVirtualFile(project)
  if (virtualFile != null) {
    WolfTheProblemSolver.getInstance(project).clearProblems(virtualFile)
  }
  if (errorHighlightLevel == EduFileErrorHighlightLevel.ALL_PROBLEMS) {
    errorHighlightLevel = EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION
  }
  YamlFormatSynchronizer.saveItem(task)
}

fun TaskFile.getSolution(): String = contents.textualRepresentation

/**
 * @return true if document related to task file has been reset, otherwise - false
 */
private fun TaskFile.resetDocument(project: Project): Boolean {
  val document = getDocument(project)
  // Note, nullable document is valid situation in case of binary files.
  if (document == null) {
    LOG.warning("Failed to find document for task file $name")
    return false
  }

  isTrackChanges = false
  document.setText(contents.textualRepresentation)
  isTrackChanges = true
  return true
}

fun TaskFile.shouldBePropagated(): Boolean = isEditable && isVisible