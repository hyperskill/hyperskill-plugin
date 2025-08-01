package org.hyperskill.academy.learning.projectView

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VfsUtilCore.getRelativePath
import com.intellij.psi.*
import com.intellij.ui.LayeredIcon
import org.hyperskill.academy.EducationalCoreIcons.CourseView
import org.hyperskill.academy.EducationalCoreIcons.CourseView.*
import org.hyperskill.academy.coursecreator.framework.SyncChangesStateManager
import org.hyperskill.academy.coursecreator.framework.SyncChangesTaskFileState
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.ext.*
import org.hyperskill.academy.learning.courseFormat.tasks.IdeTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.pathRelativeToTask
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import org.jetbrains.annotations.TestOnly
import javax.swing.Icon

object CourseViewUtils {

  fun modifyTaskChildNode(
    project: Project,
    childNode: AbstractTreeNode<*>,
    task: Task?,
    fileNodeFactory: (AbstractTreeNode<*>, PsiFile) -> AbstractTreeNode<*>,
    directoryNodeFactory: (PsiDirectory) -> AbstractTreeNode<*>,
  ): AbstractTreeNode<*>? {
    val value = childNode.value
    return when (value) {
      is PsiDirectory -> {
        val dirName = value.name
        if (dirName == EduNames.BUILD || dirName == EduNames.OUT) return null
        if (task != null && isShowDirInView(project, task, value)) directoryNodeFactory(value) else null
      }

      is PsiElement -> {
        val psiFile = value.containingFile ?: return null
        val virtualFile = psiFile.virtualFile ?: return null
        val path = virtualFile.pathRelativeToTask(project)
        val visibleFile = task?.getTaskFile(path)
        if (visibleFile?.isVisible == true) fileNodeFactory(childNode, psiFile) else null
      }

      else -> null
    }
  }

  private fun isShowDirInView(project: Project, task: Task, dir: PsiDirectory): Boolean {
    if (dir.children.isEmpty()) return true
    val dirName = dir.name
    val hasTaskFileNotInsideSourceDir = task.hasVisibleTaskFilesNotInsideSourceDir(project)
    if (dirName == task.sourceDir) return hasTaskFileNotInsideSourceDir
    return task.taskFiles.values.any {
      if (!it.isVisible) return@any false
      val virtualFile = it.getVirtualFile(project) ?: return@any false
      VfsUtil.isAncestor(dir.virtualFile, virtualFile, true)
    }
  }

  private fun Task.hasVisibleTaskFilesNotInsideSourceDir(project: Project): Boolean {
    val taskDir = getDir(project.courseDir) ?: error("Directory for task $name not found")
    val sourceDir = findSourceDir(taskDir) ?: return false
    return taskFiles.values.any {
      if (!it.isVisible) return@any false
      val virtualFile = it.getVirtualFile(project)
      if (virtualFile == null) {
        Logger.getInstance(Task::class.java).warn("VirtualFile for ${it.name} not found")
        return@any false
      }

      !VfsUtil.isAncestor(sourceDir, virtualFile, true)
    }
  }

  fun findTaskDirectory(project: Project, baseDir: PsiDirectory, task: Task): PsiDirectory? {
    val sourceDirName = task.sourceDir
    if (sourceDirName.isNullOrEmpty()) {
      return baseDir
    }
    val vFile = baseDir.virtualFile
    val sourceVFile = vFile.findFileByRelativePath(sourceDirName) ?: return baseDir

    if (task.hasVisibleTaskFilesNotInsideSourceDir(project)) {
      return baseDir
    }
    return PsiManager.getInstance(project).findDirectory(sourceVFile)
  }

  @TestOnly
  fun testPresentation(node: AbstractTreeNode<out PsiFileSystemItem>): String {
    val presentation = node.presentation
    val fragments = presentation.coloredText
    val className = node.javaClass.simpleName
    return if (fragments.isEmpty()) {
      "$className ${presentation.presentableText}"
    }
    else {
      fragments.joinToString(separator = "", prefix = "$className ") { it.text }
    }
  }

  fun getIcon(item: StudyItem): Icon {
    val icon: Icon = when (item) {
      is Course -> CourseTree
      is Section -> if (item.isSolved) SectionSolved else Section

      is Lesson -> if (item.isSolved) LessonSolved else Lesson

      is Task -> when (item) {
        is IdeTask -> if (item.isSolved) IdeTaskSolved else CourseView.IdeTask
        is TheoryTask -> if (item.isSolved) TheoryTaskSolved else CourseView.TheoryTask
        else -> if (item.status == CheckStatus.Unchecked) CourseView.Task
        else if (item.isSolved || item.containsCorrectSubmissions()) TaskSolved
        else TaskFailed
      }

      else -> error("Unexpected item type: ${item.javaClass.simpleName}")
    }
    val modifier = getSyncChangesModifier(item) ?: return icon
    return LayeredIcon.create(icon, modifier)
  }

  private fun getSyncChangesModifier(item: StudyItem): Icon? {
    val project = item.course.project ?: return null
    val syncChangesStateManager = SyncChangesStateManager.getInstance(project)
    val state = when (item) {
      is Task -> syncChangesStateManager.getSyncChangesState(item)
      is FrameworkLesson -> syncChangesStateManager.getSyncChangesState(item)
      else -> null
    }
    return when (state) {
      SyncChangesTaskFileState.INFO -> SyncFilesModInfo
      SyncChangesTaskFileState.WARNING -> SyncFilesModWarning
      else -> null
    }
  }

  val StudyItem.isSolved: Boolean
    get() {
      return when (this) {
        is Section -> lessons.all { it.isSolved() }
        is Lesson -> isSolved()
        is Task -> status == CheckStatus.Solved
        else -> false
      }
    }

  private fun Lesson.isSolved() = taskList.all {
    val project = it.project ?: return false
    it.status == CheckStatus.Solved || SubmissionsManager.getInstance(project).containsCorrectSubmission(it.id)
  }

  val Task.icon: Icon
    get() {
      return when (this) {
        is IdeTask -> if (isSolved) IdeTaskSolved else CourseView.IdeTask
        is TheoryTask -> if (isSolved) TheoryTaskSolved else CourseView.TheoryTask
        else -> if (status == CheckStatus.Unchecked) CourseView.Task
        else if (isSolved || containsCorrectSubmissions()) TaskSolved
        else TaskFailed
      }
    }

  private fun Task.containsCorrectSubmissions(): Boolean {
    val project = course.project ?: return false
    return SubmissionsManager.getInstance(project).containsCorrectSubmission(id)
  }

  fun ContentHolderNode.createNodeFromPsiDirectory(
    course: Course,
    directory: PsiDirectory,
  ): AbstractTreeNode<*>? {
    val section = course.getSection(directory.name)
    if (section != null) {
      return createSectionNode(directory, section)
    }
    val lesson = course.getLesson(directory.name)
    if (lesson != null) {
      val lessonSolved = lesson.taskList.all { it.status == CheckStatus.Solved }
      if (lessonSolved && PropertiesComponent.getInstance().getBoolean(CourseViewPane.HIDE_SOLVED_LESSONS, false)) {
        return null
      }
      return createLessonNode(directory, lesson)
    }
    if (directory.isPartOfCustomContentPath(getProject())) {
      return createIntermediateDirectoryNode(directory, course)
    }
    return null
  }

  private fun PsiDirectory.isPartOfCustomContentPath(project: Project): Boolean {
    val relativePath = getRelativePath(virtualFile, project.courseDir) ?: return false
    return project.course.customContentPath.contains(relativePath)
  }
}
