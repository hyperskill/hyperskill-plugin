package org.hyperskill.academy.learning

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtilCore.VFS_SEPARATOR_CHAR
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.FileInfo.FileOutsideTasks
import org.hyperskill.academy.learning.configuration.excludeFromArchive
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.tasks.Task

fun VirtualFile.fileInfo(project: Project): FileInfo? {
  if (project.isDisposed) return null

  if (isDirectory) {
    getSection(project)?.let { return FileInfo.SectionDirectory(it) }
    getLesson(project)?.let { return FileInfo.LessonDirectory(it) }
    getTask(project)?.let { return FileInfo.TaskDirectory(it) }
  }

  val task = getContainingTask(project)

  if (task == null) {
    val course = project.course ?: return null
    val relativePath = FileUtil.getRelativePath(
      CourseInfoHolder.fromCourse(course, project.courseDir).courseDir.path,
      this.path,
      VFS_SEPARATOR_CHAR
    ) ?: return null
    return FileOutsideTasks(course, relativePath)
  }

  if (shouldIgnore(this, project, task)) return null

  val taskRelativePath = pathRelativeToTask(project)

  return FileInfo.FileInTask(task, taskRelativePath)
}

private fun shouldIgnore(file: VirtualFile, project: Project, task: Task): Boolean {
  val courseDir = project.courseDir
  if (!FileUtil.isAncestor(courseDir.path, file.path, true)) return true
  val course = StudyTaskManager.getInstance(project).course ?: return true
  return course.configurator?.excludeFromArchive(project, file) == true
}

sealed class FileInfo {
  data class SectionDirectory(val section: Section) : FileInfo()
  data class LessonDirectory(val lesson: Lesson) : FileInfo()
  data class TaskDirectory(val task: Task) : FileInfo()
  data class FileInTask(val task: Task, val pathInTask: String) : FileInfo()
  data class FileOutsideTasks(val course: Course, val coursePath: String) : FileInfo()
}
