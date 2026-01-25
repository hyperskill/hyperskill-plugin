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

  fun saveExternalChanges(task: Task, externalState: Map<String, String>, submissionId: Long? = null)
  fun updateUserChanges(task: Task, newInitialState: Map<String, String>)

  /**
   * Adds new files to an existing snapshot without overwriting existing files.
   * Used when server adds new template files to a task that user has already visited.
   *
   * @param task the task whose snapshot should be updated
   * @param newFiles map of new file paths to their contents
   */
  fun addNewFilesToSnapshot(task: Task, newFiles: Map<String, String>)

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
   * Note: This method does NOT overwrite existing cache entries to protect API data.
   * Use [updateOriginalTestFiles] when you need to update cache with fresh data (e.g., after course update).
   *
   * @param task the task whose test files should be stored
   */
  fun storeOriginalTestFiles(task: Task)

  /**
   * Updates original test files cache for a task, overwriting any existing cached data.
   * Use this method when updating test files from remote server (e.g., during course update).
   * Unlike [storeOriginalTestFiles], this method WILL overwrite existing cache entries.
   *
   * @param task the task whose test files cache should be updated
   */
  fun updateOriginalTestFiles(task: Task)

  /**
   * Updates original template files cache for a task, overwriting any existing cached data.
   * Use this method when updating template files from remote server (e.g., during course update).
   * Unlike [storeOriginalTemplateFiles], this method WILL overwrite existing cache entries.
   *
   * @param task the task whose template files cache should be updated
   */
  fun updateOriginalTemplateFiles(task: Task)

  /**
   * Stores original template files (visible non-test files) from API for a task.
   * These templates are used for calculating user changes correctly in [saveExternalChanges].
   * Should be called after creating tasks from API response.
   *
   * IMPORTANT: Call this when task files are loaded from API (fresh data).
   * Do NOT call after user may have modified files, as TaskFile.contents may be stale.
   *
   * @param task the task whose template files should be stored
   */
  fun storeOriginalTemplateFiles(task: Task)

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

  /**
   * Ensures template files (visible non-test files) are cached for a task, loading from API if necessary.
   * This should be called before [saveExternalChanges] when the cache might be empty
   * (e.g., after IDE restart when data is loaded from YAML instead of API).
   *
   * Unlike [storeOriginalTemplateFiles], this method loads fresh data from API
   * instead of using potentially stale task.taskFiles.
   *
   * @param task the task whose template files should be cached
   * @return true if template files are now cached (either already were or successfully loaded from API),
   *         false if loading from API failed
   */
  fun ensureTemplateFilesCached(task: Task): Boolean

  /**
   * Synchronizes the lesson's currentTaskIndex with the storage HEAD.
   * This should be called when opening a project to ensure the in-memory state
   * matches the persisted storage state.
   *
   * @param lesson the framework lesson to synchronize
   * @return true if synchronization was successful, false if HEAD is not set or doesn't match any task
   */
  fun syncCurrentTaskIndexFromStorage(lesson: FrameworkLesson): Boolean

  companion object {
    fun getInstance(project: Project): FrameworkLessonManager = project.service()
  }
}
