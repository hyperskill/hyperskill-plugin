package org.hyperskill.academy.learning.framework

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.EduTestAware
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.tasks.Task

interface FrameworkLessonManager : EduTestAware {
  fun prepareNextTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean)
  fun preparePrevTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean)

  fun saveExternalChanges(task: Task, externalState: Map<String, String>)
  fun updateUserChanges(task: Task, newInitialState: Map<String, String>)

  fun getChangesTimestamp(task: Task): Long

  /**
   * Retrieves the state of a given task in a framework lesson.
   *
   * @param lesson the framework lesson containing the task
   * @param task the task for which to retrieve the state
   * @return a map representing the state of the task, where the keys are the file paths, and the values are the file contents
   */
  fun getTaskState(lesson: FrameworkLesson, task: Task): Map<String, String>

  /**
   * Stores original test files from API for a task.
   * These files will be used to recreate test files with correct content when navigating between stages.
   * Should be called after creating tasks from API response.
   *
   * @param task the task whose test files should be stored
   */
  fun storeOriginalTestFiles(task: Task)

  companion object {
    fun getInstance(project: Project): FrameworkLessonManager = project.service()
  }
}
