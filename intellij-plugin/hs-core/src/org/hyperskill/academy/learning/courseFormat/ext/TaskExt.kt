@file:JvmName("TaskExt")

package org.hyperskill.academy.learning.courseFormat.ext

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ApplicationNamesInfo
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtilCore
import com.intellij.util.concurrency.annotations.RequiresReadLock
import org.hyperskill.academy.learning.EduUtilsKt.convertToHtml
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.TASK
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.getTextFromTaskTextFile
import org.hyperskill.academy.learning.isTestsFile
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.selectedTaskFile
import org.hyperskill.academy.learning.taskToolWindow.removeHyperskillTags
import org.hyperskill.academy.learning.taskToolWindow.replaceActionIDsWithShortcuts
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.hyperskill.academy.learning.yaml.errorHandling.loadingError
import java.io.IOException

val Task.project: Project? get() = course.project

val Task.sourceDir: String? get() = course.sourceDir
val Task.testDirs: List<String> get() = course.testDirs

val Task.isFrameworkTask: Boolean get() = parentOrNull is FrameworkLesson

val Task.dirName: String
  get() {
    return TASK
  }

fun Task.findDir(lessonDir: VirtualFile?): VirtualFile? {
  return lessonDir?.findChild(dirName)
}

fun Task.findSourceDir(taskDir: VirtualFile): VirtualFile? {
  val sourceDir = sourceDir ?: return null
  return taskDir.findFileByRelativePath(sourceDir)
}

fun Task.findTestDirs(taskDir: VirtualFile): List<VirtualFile> = testDirs.mapNotNull { taskDir.findFileByRelativePath(it) }

fun Task.findTestDirs(project: Project): List<VirtualFile> {
  val taskDir = getDir(project.courseDir) ?: return emptyList()
  return findTestDirs(taskDir)
}

fun Task.getAllTestDirectories(project: Project): List<PsiDirectory> {
  val testDirs = findTestDirs(project)
  return testDirs.mapNotNull { PsiManager.getInstance(project).findDirectory(it) }
}

fun Task.getAllTestFiles(project: Project): List<PsiFile> {
  val testFiles = getAllTestVFiles(project)
  return PsiUtilCore.toPsiFiles(PsiManager.getInstance(project), testFiles)
}

fun Task.getAllTestVFiles(project: Project): MutableList<VirtualFile> {
  val testFiles = mutableListOf<VirtualFile>()
  findTestDirs(project).forEach { testDir ->
    VfsUtilCore.processFilesRecursively(testDir) {
      if (!it.isDirectory && it.isTestsFile(project)) {
        testFiles.add(it)
      }
      true
    }
  }
  return testFiles
}

fun Task.hasChangedFiles(project: Project): Boolean {
  for (taskFile in taskFiles.values) {
    val document = taskFile.getDocument(project) ?: continue
    if (document.text != taskFile.contents.textualRepresentation) {
      return true
    }
  }
  return false
}

fun Task.saveStudentAnswersIfNeeded(project: Project) {
  if (parentOrNull !is FrameworkLesson) return

  getDir(project.courseDir) ?: return
  YamlFormatSynchronizer.saveItem(this)
}

@RequiresReadLock
fun Task.getDescriptionFile(
  project: Project,
  guessFormat: Boolean = false
): VirtualFile? {
  val taskDirectory = getTaskDirectory(project) ?: return null

  val file = com.intellij.util.SlowOperations.knownIssue("EDU-8086").use {
    if (guessFormat) {
      taskDirectory.run { findChild(DescriptionFormat.HTML.fileName) ?: findChild(DescriptionFormat.MD.fileName) }
    }
    else {
      taskDirectory.findChild(descriptionFormat.fileName)
    }
  }
  if (file == null) {
    LOG.warn("No task description file for $name")
  }
  return file
}

fun Task.canShowSolution(): Boolean {
  return hasSolutions() && status == CheckStatus.Solved
}

fun Task.hasSolutions(): Boolean = this !is TheoryTask

fun Task.getCodeTaskFile(project: Project): TaskFile? {

  fun String.getCodeTaskFile(): TaskFile? {
    val name = GeneratorUtils.joinPaths(sourceDir, this)
    return taskFiles[name]
  }

  val files = taskFiles.values
  if (files.size == 1) return files.firstOrNull()
  course.configurator?.courseBuilder?.mainTemplateName(course)?.getCodeTaskFile()?.let { return it }
  course.configurator?.courseBuilder?.taskTemplateName(course)?.getCodeTaskFile()?.let { return it }
  val editorTaskFile = project.selectedTaskFile
  return if (editorTaskFile?.task == this) {
    editorTaskFile
  }
  else {
    files.firstOrNull { !it.isLearnerCreated && it.isVisible }
  }
}

fun Task.revertTaskFiles(project: Project) {
  ApplicationManager.getApplication().runWriteAction {
    for (taskFile in taskFiles.values) {
      taskFile.revert(project)
    }
  }
}

fun Task.revertTaskParameters() {
  status = CheckStatus.Unchecked
}

@RequiresReadLock
fun Task.updateDescriptionTextAndFormat(project: Project) = runReadAction {
  val taskDescriptionFile = getDescriptionFile(project, guessFormat = true)

  if (taskDescriptionFile == null) {
    descriptionFormat = DescriptionFormat.HTML
    descriptionText = EduCoreBundle.message("task.description.not.found")
    return@runReadAction
  }

  try {
    descriptionText = VfsUtil.loadText(taskDescriptionFile)
    descriptionFormat = taskDescriptionFile.toDescriptionFormat()
  }
  catch (_: IOException) {
    LOG.warn("Failed to load text " + taskDescriptionFile.name)
    descriptionFormat = DescriptionFormat.HTML
    descriptionText = EduCoreBundle.message("task.description.not.found")
  }
}

private fun VirtualFile.toDescriptionFormat(): DescriptionFormat =
  DescriptionFormat.values().firstOrNull { it.extension == extension }
  ?: loadingError(EduCoreBundle.message("yaml.editor.invalid.description"))

fun Task.getFormattedTaskText(project: Project): String? {
  var text = runReadAction {
    getTaskText(project)
  } ?: return null

  text = StringUtil.replace(text, "%IDE_NAME%", ApplicationNamesInfo.getInstance().fullProductName)
  val textBuffer = StringBuffer(text)
  replaceActionIDsWithShortcuts(textBuffer)
  if (course is HyperskillCourse) {
    removeHyperskillTags(textBuffer)
  }
  return textBuffer.toString()
}

/**
 * In learner mode in framework lessons tasks, `getDir(project.courseDir)`
 * returns the path to the `lesson/task` folder where the task files for the current task are stored.
 *
 * But the task description and YAML files for the task are in the folder with the task name (f. e. `lesson/task1`)
 */
fun Task.getTaskDirectory(project: Project): VirtualFile? {
  val lesson = parentOrNull as? Lesson
  val taskDirectory = if (lesson is FrameworkLesson) {
    lesson.getDir(project.courseDir)?.findChild(name)
  }
  else {
    getDir(project.courseDir)
  }
  if (taskDirectory == null) {
    LOG.warn("Cannot find task directory for a task: $name")
  }
  return taskDirectory
}

@RequiresReadLock
fun Task.getTaskText(project: Project): String? {
  val taskTextFile = getDescriptionFile(project, guessFormat = true) ?: return null
  val taskDescription = taskTextFile.getTextFromTaskTextFile() ?: return descriptionText

  if (taskTextFile.extension == DescriptionFormat.MD.extension) {
    return convertToHtml(taskDescription)
  }

  return taskDescription
}

private val LOG = logger<Task>()