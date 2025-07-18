package com.jetbrains.edu.learning

import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.edu.coursecreator.framework.SyncChangesStateManager
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.EduFileErrorHighlightLevel
import com.jetbrains.edu.learning.courseFormat.TaskFile
import com.jetbrains.edu.learning.courseFormat.ext.getVirtualFile
import com.jetbrains.edu.learning.courseFormat.ext.project
import com.jetbrains.edu.learning.courseFormat.tasks.Task

/**
 * Listens changes in study files and updates
 * coordinates of all the placeholders in current task file
 */
class EduDocumentListener private constructor(
  holder: CourseInfoHolder<out Course?>,
  /**
   * If [taskFile] is `null` than listener should determine affected task file by [DocumentEvent],
   * otherwise, it should track changes only in single [Document] related to [taskFile]
   */
  private val taskFile: TaskFile?
) : EduDocumentListenerBase(holder) {

  override fun beforeDocumentChange(e: DocumentEvent) {
    if (taskFile == null && !e.isInProjectContent()) return
    val taskFile = (taskFile ?: e.taskFile) ?: return
    if (!taskFile.isTrackChanges) {
      return
    }
    if (taskFile.errorHighlightLevel == EduFileErrorHighlightLevel.TEMPORARY_SUPPRESSION) {
      taskFile.errorHighlightLevel = EduFileErrorHighlightLevel.ALL_PROBLEMS

      val project = holder.course?.project
      if (project != null) {
        taskFile.getVirtualFile(project)?.setHighlightLevel(project, taskFile.errorHighlightLevel)
      }
    }
  }

  override fun documentChanged(e: DocumentEvent) {
    if (taskFile == null && !e.isInProjectContent()) return
    val taskFile = (taskFile ?: e.taskFile) ?: return
    if (!taskFile.isTrackChanges) {
      return
    }

    val project = holder.course?.project
    if (project != null) {
      SyncChangesStateManager.getInstance(project).taskFileChanged(taskFile)
    }
  }

  private val DocumentEvent.taskFile: TaskFile?
    get() {
      val file = FileDocumentManager.getInstance().getFile(document) ?: return null
      return file.getTaskFile(holder)
    }

  companion object {
    fun setGlobalListener(project: Project, disposable: Disposable) {
      EditorFactory.getInstance().eventMulticaster.addDocumentListener(EduDocumentListener(project.toCourseInfoHolder(), null), disposable)
    }

    /**
     * Should be used only when current course doesn't contain task file related to given [file].
     * For example, when changes are performed on non-physical file.
     */
    fun runWithListener(project: Project, taskFile: TaskFile, file: VirtualFile, action: (Document) -> Unit) {
      return runWithListener(project.toCourseInfoHolder(), taskFile, file, action)
    }

    /**
     * Should be used only when current course doesn't contain task file related to given [file].
     * For example, when changes are performed on non-physical file.
     */
    fun runWithListener(holder: CourseInfoHolder<out Course?>, taskFile: TaskFile, file: VirtualFile, action: (Document) -> Unit) {
      require(file.getTaskFile(holder) == null) {
        "Changes in `${taskFile.name}` should be tracked by global listener"
      }
      val document = FileDocumentManager.getInstance().getDocument(file) ?: return

      val listener = EduDocumentListener(holder, taskFile)
      document.addDocumentListener(listener)
      try {
        action(document)
      }
      finally {
        document.removeDocumentListener(listener)
      }
    }

    fun modifyWithoutListener(task: Task, pathInTask: String, modification: () -> Unit) {
      val taskFile = task.getTaskFile(pathInTask) ?: return
      val isTrackChanges = taskFile.isTrackChanges
      taskFile.isTrackChanges = false
      try {
        modification()
      }
      finally {
        taskFile.isTrackChanges = isTrackChanges
      }
    }
  }
}
