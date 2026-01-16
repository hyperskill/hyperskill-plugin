package org.hyperskill.academy.learning.framework

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.EduTestAware
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.TaskFile
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

  /**
   * Retrieves cached original test files for a task.
   * These are the test files that were stored from API response when the task was created/updated.
   *
   * @param task the task whose test files should be retrieved
   * @return collection of cached test files, or null if no cached files exist for this task
   */
  fun getOriginalTestFiles(task: Task): Collection<TaskFile>?

  /**
   * Ensures test files are cached for a task, loading from API if necessary.
   * This should be called before using [getOriginalTestFiles] when the cache might be empty
   * (e.g., after IDE restart when data is loaded from YAML instead of API).
   *
   * IMPORTANT (ALT-10961): Never falls back to task.taskFiles as it may be corrupted
   * with test content from another stage in framework lessons.
   *
   * @param task the task whose test files should be cached
   * @return true if test files are now cached (either already were or successfully loaded from API),
   *         false if loading from API failed
   */
  fun ensureTestFilesCached(task: Task): Boolean

  companion object {
    fun getInstance(project: Project): FrameworkLessonManager = project.service()
  }
}
