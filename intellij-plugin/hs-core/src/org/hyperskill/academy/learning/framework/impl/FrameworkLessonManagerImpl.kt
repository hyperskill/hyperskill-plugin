package org.hyperskill.academy.learning.framework.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SlowOperations
import com.intellij.util.io.storage.AbstractStorage
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.isTestFile
import org.hyperskill.academy.learning.courseFormat.ext.shouldBePropagated
import org.hyperskill.academy.learning.courseFormat.ext.testDirs
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.hyperskill.academy.learning.framework.FrameworkStorageListener
import org.hyperskill.academy.learning.framework.propagateFilesOnNavigation
import org.hyperskill.academy.learning.framework.storage.Change
import org.hyperskill.academy.learning.framework.storage.SnapshotDiff
import org.hyperskill.academy.learning.framework.storage.UserChanges
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.PyCharmStepOptions
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.toCourseInfoHolder
import org.hyperskill.academy.learning.ui.getUIName
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Keeps list of [Change]s for each task. Change list is difference between initial task state and latest one.
 *
 * Allows navigating between tasks in framework lessons in learner mode (where only current task is visible for a learner)
 * without rewriting whole task content.
 * It can be essential in large projects like Android applications where a lot of files are the same between two consecutive tasks
 */
class FrameworkLessonManagerImpl(private val project: Project) : FrameworkLessonManager, Disposable {
  private var storage: FrameworkStorage = createStorage(project)

  // Cache of original test files from API for each task (by step ID)
  // These are used to recreate test files with correct content when navigating between stages
  private val originalTestFilesCache = java.util.concurrent.ConcurrentHashMap<Int, Map<String, TaskFile>>()

  // Cache of original template files (visible non-test files) for each task (by step ID)
  // These are used to calculate user changes correctly, since TaskFile.contents may be modified
  private val originalTemplateFilesCache = java.util.concurrent.ConcurrentHashMap<Int, Map<String, String>>()

  override fun prepareNextTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean) {
    applyTargetTaskChanges(lesson, 1, taskDir, showDialogIfConflict)
  }

  override fun preparePrevTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean) {
    applyTargetTaskChanges(lesson, -1, taskDir, showDialogIfConflict)
  }

  private fun getParentRefId(task: Task): Int {
    val lesson = task.lesson as? FrameworkLesson ?: return -1
    val taskIndex = task.index
    return if (taskIndex > 1) {
      lesson.taskList.getOrNull(taskIndex - 2)?.record ?: -1
    } else -1
  }

  override fun saveExternalChanges(task: Task, externalState: Map<String, String>) {
    require(task.lesson is FrameworkLesson) {
      "Only solutions of framework tasks can be saved"
    }

    LOG.warn("saveExternalChanges: task='${task.name}', externalState.keys=${externalState.keys}")

    // Filter external state to only include propagatable files (exclude test files, etc.)
    val externalPropagatableFiles = externalState.split(task).first
    LOG.warn("saveExternalChanges: externalPropagatableFiles.keys=${externalPropagatableFiles.keys}")

    val currentRefId = task.record
    val parentRefId = if (currentRefId == -1) getParentRefId(task) else -1

    // Save the API state as a snapshot
    var newRefId = try {
      storage.saveSnapshot(currentRefId, externalPropagatableFiles, parentRefId)
    }
    catch (e: IOException) {
      LOG.error("Failed to save solution for task `${task.name}`", e)
      currentRefId
    }

    // Migration: If legacy storage has changes for this task's old refId, apply them on top.
    // This preserves user's local changes that weren't submitted to the server.
    if (currentRefId != -1 && storage.hasLegacyChanges(currentRefId)) {
      LOG.info("Migrating legacy changes for task '${task.name}' (refId=$currentRefId)")
      newRefId = try {
        storage.applyLegacyChangesAndSave(newRefId, externalPropagatableFiles)
      }
      catch (e: IOException) {
        LOG.error("Failed to apply legacy changes for task `${task.name}`", e)
        newRefId
      }
    }

    task.record = newRefId
    LOG.warn("saveExternalChanges: task='${task.name}', record updated from $currentRefId to ${task.record}, parentRefId=$parentRefId")
    YamlFormatSynchronizer.saveItem(task)
  }

  override fun updateUserChanges(task: Task, newInitialState: Map<String, String>) {
    // No-op: With snapshot-based storage, we don't need to update change types.
    // We store full snapshots and calculate diffs on-the-fly when needed.
    // The diff calculation uses the current initial state, so it always produces correct change types.
  }

  override fun getChangesTimestamp(task: Task): Long {
    require(task.lesson is FrameworkLesson) {
      "Changes timestamp makes sense only for framework tasks"
    }

    return try {
      storage.getSnapshotTimestamp(task.record)
    }
    catch (e: IOException) {
      LOG.warn("Failed to get snapshot timestamp for task `${task.name}`", e)
      0L
    }
  }

  override fun getTaskState(lesson: FrameworkLesson, task: Task): Map<String, String> {
    require(task.lesson == lesson) {
      "The task is not a part of this lesson"
    }
    val initialFiles = task.allFiles
    val changes = if (lesson.currentTaskIndex + 1 == task.index) {
      val taskDir = task.getDir(project.courseDir) ?: return emptyMap()
      getUserChangesFromFiles(initialFiles, taskDir)
    }
    else {
      getUserChangesFromStorage(task)
    }
    return HashMap(initialFiles).apply { changes.apply(this) }
  }

  /**
   * Convert the current state on local FS related to current task in framework lesson
   * to a new one, to get state of next/previous (target) task.
   */
  private fun applyTargetTaskChanges(
    lesson: FrameworkLesson,
    taskIndexDelta: Int,
    taskDir: VirtualFile,
    showDialogIfConflict: Boolean
  ) {
    val currentTaskIndex = lesson.currentTaskIndex
    val targetTaskIndex = currentTaskIndex + taskIndexDelta

    val currentTask = lesson.taskList[currentTaskIndex]
    val targetTask = lesson.taskList[targetTaskIndex]

    LOG.info("=== Stage switch: '${currentTask.name}' (idx=$currentTaskIndex) -> '${targetTask.name}' (idx=$targetTaskIndex), delta=$taskIndexDelta ===")

    lesson.currentTaskIndex = targetTaskIndex
    SlowOperations.knownIssue("EDU-XXXX").use {
      YamlFormatSynchronizer.saveItem(lesson)
    }

    val currentRefId = currentTask.record
    val targetRefId = targetTask.record

    // ALT-10961: Try to load template files from API if cache is empty.
    // This ensures we have correct templates for diff calculation.
    // We load synchronously to ensure cache is filled before navigation proceeds.
    if (!originalTemplateFilesCache.containsKey(currentTask.id)) {
      if (ApplicationManager.getApplication().isDispatchThread) {
        // On EDT - run API call on pooled thread and wait for result
        try {
          ApplicationManager.getApplication().executeOnPooledThread<Unit> {
            loadTemplateFilesFromApiSync(currentTask)
          }.get()
        } catch (e: Exception) {
          LOG.warn("Failed to load template files from API for task '${currentTask.name}'", e)
        }
      } else {
        loadTemplateFilesFromApiSync(currentTask)
      }
    }
    val cachedTemplates = originalTemplateFilesCache[currentTask.id]
    val hasValidTemplateCache = cachedTemplates != null
    LOG.warn("Navigation: currentTask='${currentTask.name}', hasValidTemplateCache=$hasValidTemplateCache, currentRefId=$currentRefId")

    val initialCurrentFiles = if (hasValidTemplateCache) {
      cachedTemplates
    } else {
      // No cached templates - we cannot correctly calculate user changes
      // Use Task.allFiles as a fallback for state calculations, but DON'T update storage
      currentTask.allFiles
    }

    // 1. Get difference between initial state of current task and previous task state
    // and construct previous state of current task.
    // Previous state is needed to determine if a user made any new change
    val previousCurrentUserChanges = getUserChangesFromStorage(currentTask)
    val previousCurrentState = HashMap(initialCurrentFiles).apply { previousCurrentUserChanges.apply(this) }

    // 2. Save FULL state (snapshot) to storage for the current task.
    // ALT-10961: We save complete state snapshots, not diffs, because:
    // 1. After IDE restart, templates may not be available (cache empty, API may fail)
    // 2. Diffs (ChangeFile) require correct templates to apply
    // 3. Full snapshots are self-contained and don't need templates
    val (newCurrentRefId, currentUserChanges) = if (hasValidTemplateCache) {
      // We have cached templates - save full current disk state as a snapshot
      val currentDiskState = getTaskStateFromFiles(initialCurrentFiles.keys, taskDir)
      val (propagatableFiles, _) = currentDiskState.split(currentTask)
      try {
        saveSnapshot(currentRefId, propagatableFiles, getParentRefId(currentTask), initialCurrentFiles)
      }
      catch (e: IOException) {
        LOG.error("Failed to save snapshot for task `${currentTask.name}`", e)
        UpdatedUserChanges(currentRefId, UserChanges.empty())
      }
    } else {
      // ALT-10961: No cached templates - DON'T save new changes.
      // After IDE restart, the disk content might be from a different stage.
      // Saving current disk state would overwrite the correct submission-based changes.
      // Use existing storage changes which were saved by loadSolutionsInBackground.
      LOG.warn("Navigation: No cached templates for '${currentTask.name}', using existing storage changes (not updating)")
      UpdatedUserChanges(currentRefId, previousCurrentUserChanges)
    }

    // 3. Update ref ID if it changed.
    if (currentTask.record != newCurrentRefId) {
      currentTask.record = newCurrentRefId
    }
    SlowOperations.knownIssue("EDU-XXXX").use {
      YamlFormatSynchronizer.saveItem(currentTask)
    }

    // 4. Get difference (change list) between initial and latest states of target task
    val nextUserChanges = getUserChangesFromStorage(targetTask)
    LOG.warn("Navigation: targetTask='${targetTask.name}', record=${targetTask.record}, nextUserChanges=${nextUserChanges.changes.size}")
    nextUserChanges.changes.forEach { change ->
      LOG.warn("Navigation: change=${change.javaClass.simpleName}(${change.path}, ${change.text.length} chars)")
    }

    // 5. Apply change lists to initial state to get latest states of current and target tasks
    val currentState = HashMap(initialCurrentFiles).apply { currentUserChanges.apply(this) }
    LOG.warn("Navigation: currentState=${currentState.mapValues { "${it.key}:${it.value.length}chars" }}")

    // ALT-10961: For target task, also try to load templates from API if cache is empty.
    // This ensures we have correct templates for state calculations after IDE restart.
    if (!originalTemplateFilesCache.containsKey(targetTask.id)) {
      if (ApplicationManager.getApplication().isDispatchThread) {
        try {
          ApplicationManager.getApplication().executeOnPooledThread<Unit> {
            loadTemplateFilesFromApiSync(targetTask)
          }.get()
        } catch (e: Exception) {
          LOG.warn("Failed to load template files from API for target task '${targetTask.name}'", e)
        }
      } else {
        loadTemplateFilesFromApiSync(targetTask)
      }
    }
    val cachedTargetTemplates = originalTemplateFilesCache[targetTask.id]
    val initialTargetFiles = cachedTargetTemplates ?: targetTask.allFiles
    LOG.warn("Navigation: initialTargetFiles=${initialTargetFiles.keys}, hasCachedTemplates=${cachedTargetTemplates != null}")
    val targetState = HashMap(initialTargetFiles).apply { nextUserChanges.apply(this) }
    LOG.warn("Navigation: targetState=${targetState.mapValues { "${it.key}:${it.value.length}chars" }}")

    // 6. Calculate difference between latest states of current and target tasks
    // Note, there are special rules for hyperskill courses for now
    // All user changes from the current task should be propagated to next task as is

    // If a user navigated back to current task, didn't make any change and wants to navigate to next task again
    // we shouldn't try to propagate current changes to next task
    val currentTaskHasNewUserChanges = !(currentRefId != -1 && targetRefId != -1 && previousCurrentState == currentState)

    val changes = if (currentTaskHasNewUserChanges && taskIndexDelta == 1 && lesson.propagateFilesOnNavigation) {
      calculatePropagationChanges(targetTask, currentTask, currentState, targetState, showDialogIfConflict)
    }
    else {
      calculateChanges(currentState, targetState)
    }

    // 7. Apply difference between latest states of current and target tasks on local FS
    changes.apply(project, taskDir, targetTask)

    // 8. Recreate test files (files with isLearnerCreated = false) from target task definition
    // These files are not tracked in framework storage, so we need to recreate them explicitly
    recreateTestFiles(project, taskDir, currentTask, targetTask)

    // 9. ALT-10961: Force save all documents and refresh VFS to ensure changes are visible in editor
    // Document changes may be in memory but not persisted or reflected in the editor
    invokeAndWaitIfNeeded {
      FileDocumentManager.getInstance().saveAllDocuments()
      VfsUtil.markDirtyAndRefresh(false, true, true, taskDir)
      LOG.info("Navigation: Saved documents and refreshed VFS for taskDir=${taskDir.path}")
    }

    SlowOperations.knownIssue("EDU-XXXX").use {
      YamlFormatSynchronizer.saveItem(targetTask)
    }

    // Update HEAD to point to the current (target) task
    if (targetTask.record != -1) {
      storage.head = targetTask.record
      LOG.info("HEAD updated to ref ${targetTask.record} (task '${targetTask.name}')")
      project.messageBus.syncPublisher(FrameworkStorageListener.TOPIC).headUpdated(targetTask.record)
    }

    LOG.info("=== Stage switch complete ===")
  }

  /**
   * Loads test files from API for a task and caches them.
   * This is used as a fallback when the cache is empty (e.g., after IDE restart).
   *
   * IMPORTANT: This method must NOT block EDT. If called from EDT, it starts async loading
   * and returns null immediately. The caller should use task.taskFiles as fallback.
   *
   * @param task the task whose test files should be loaded
   * @return map of test files (filename -> TaskFile), or null if loading failed or is in progress
   */
  private fun loadTestFilesFromApi(task: Task): Map<String, TaskFile>? {
    val stepId = task.id
    if (stepId <= 0) {
      LOG.warn("Cannot load test files from API: task '${task.name}' has invalid step ID: $stepId")
      return null
    }

    // Don't block EDT - start async loading and return null
    // The caller will use task.taskFiles as fallback
    if (ApplicationManager.getApplication().isDispatchThread) {
      LOG.info("On EDT, starting async load for test files of task '${task.name}' (step $stepId)")
      ApplicationManager.getApplication().executeOnPooledThread {
        loadTestFilesFromApiSync(task)
      }
      return null
    }

    return loadTestFilesFromApiSync(task)
  }

  /**
   * Synchronously loads test files from API. Must NOT be called from EDT.
   */
  private fun loadTestFilesFromApiSync(task: Task): Map<String, TaskFile>? {
    val stepId = task.id
    LOG.info("Loading test files from API for task '${task.name}' (step $stepId)")

    return try {
      // ALT-10961: Use anonymous request to get original test files from API.
      // Authenticated requests return files from user's last submission instead of original stage files.
      val stepSourceResult = HyperskillConnector.getInstance().getStepSourceAnonymous(stepId)
      if (stepSourceResult is Err) {
        LOG.warn("Failed to load step source from API for step $stepId: ${stepSourceResult.error}")
        return null
      }

      val stepSource = (stepSourceResult as Ok).value
      val options = stepSource.block?.options as? PyCharmStepOptions
      if (options == null) {
        LOG.warn("Step $stepId has no PyCharmStepOptions")
        return null
      }

      val allFiles = options.files
      if (allFiles.isNullOrEmpty()) {
        LOG.warn("Step $stepId has no files in options")
        return null
      }

      // Filter test files (not learner-created and likely a test file by name)
      // Note: Cannot use taskFile.isTestFile here because TaskFile objects from API
      // are not attached to a Task, and isTestFile requires task context.
      val testFiles = allFiles.filter { taskFile ->
        !taskFile.isLearnerCreated && "test" in taskFile.name.lowercase()
      }

      if (testFiles.isEmpty()) {
        LOG.warn("Step $stepId has no test files")
        return null
      }

      // Create copies of TaskFile objects and cache them
      // Force isVisible = false for test files so they don't appear in Project View
      val copiedTestFiles = testFiles.associateBy(
        { it.name },
        { taskFile ->
          TaskFile(taskFile.name, taskFile.contents).also {
            it.isVisible = false
            it.isEditable = taskFile.isEditable
            it.isLearnerCreated = taskFile.isLearnerCreated
          }
        }
      )

      originalTestFilesCache[stepId] = copiedTestFiles
      val filesInfo = copiedTestFiles.entries.joinToString { (name, file) ->
        "$name:size=${file.contents.textualRepresentation.length}"
      }
      LOG.info("Loaded and cached ${copiedTestFiles.size} test files from API for task '${task.name}' (step $stepId): [$filesInfo]")

      copiedTestFiles
    }
    catch (e: Exception) {
      LOG.warn("Exception while loading test files from API for step $stepId", e)
      null
    }
  }

  /**
   * Recreates test files (files with isLearnerCreated = false) from the cached task definition.
   * These files are provided by the course author and should not be modified by students.
   * We recreate them when navigating to a new task to ensure they match the task definition.
   *
   * IMPORTANT (ALT-10961): Only uses test files from [originalTestFilesCache] which is populated
   * by [storeOriginalTestFiles] when fresh API data is received. Does NOT fall back to task.taskFiles
   * because in framework lessons, task.taskFiles may be corrupted with test content from another stage
   * (all stages share the same task directory on disk).
   *
   * @param currentTask The task we're navigating FROM (used to identify old test files to delete)
   * @param targetTask The task we're navigating TO (used to identify new test files to create)
   */
  private fun recreateTestFiles(project: Project, taskDir: VirtualFile, currentTask: Task, targetTask: Task) {
    // Get old test files from current task cache to determine which ones need to be deleted
    var cachedCurrentTestFiles = originalTestFilesCache[currentTask.id]
    // Fallback to task.taskFiles for current task when cache is empty
    if (cachedCurrentTestFiles == null) {
      val taskTestDirs = currentTask.testDirs
      cachedCurrentTestFiles = currentTask.taskFiles.filterValues { taskFile ->
        !taskFile.isLearnerCreated && !taskFile.isVisible &&
        taskTestDirs.any { testDir -> taskFile.name.startsWith("$testDir/") || taskFile.name == testDir }
      }
    }
    val currentTestFileNames = cachedCurrentTestFiles.keys

    // ALT-10961: Only use cached test files from API to prevent using corrupted task.taskFiles
    // Cache is populated by storeOriginalTestFiles() when fresh API data is received.
    // DO NOT update cache from task.taskFiles here - it may contain test content from another stage
    // due to framework lesson stages sharing the same task directory.
    var cachedTargetTestFiles = originalTestFilesCache[targetTask.id]

    if (cachedTargetTestFiles == null) {
      LOG.warn("No cached test files for task '${targetTask.name}' (step ${targetTask.id}). Attempting to load from API...")
      // Load synchronously to ensure files are available before recreating
      if (ApplicationManager.getApplication().isDispatchThread) {
        try {
          ApplicationManager.getApplication().executeOnPooledThread<Unit> {
            loadTestFilesFromApiSync(targetTask)
          }.get()
          cachedTargetTestFiles = originalTestFilesCache[targetTask.id]
        } catch (e: Exception) {
          LOG.warn("Failed to load test files from API for task '${targetTask.name}'", e)
        }
      } else {
        cachedTargetTestFiles = loadTestFilesFromApi(targetTask)
      }
    }

    // Fallback to task.taskFiles when cache is empty and API failed (e.g., in tests or offline mode)
    // This is less reliable but better than not creating test files at all
    if (cachedTargetTestFiles == null) {
      LOG.warn("API also failed. Falling back to task.taskFiles for task '${targetTask.name}'")
      val taskTestDirs = targetTask.testDirs
      val testFilesFromTask = targetTask.taskFiles.filterValues { taskFile ->
        !taskFile.isLearnerCreated && !taskFile.isVisible &&
        taskTestDirs.any { testDir -> taskFile.name.startsWith("$testDir/") || taskFile.name == testDir }
      }
      if (testFilesFromTask.isNotEmpty()) {
        cachedTargetTestFiles = testFilesFromTask
        LOG.info("Using ${testFilesFromTask.size} test files from task.taskFiles for task '${targetTask.name}'")
      }
    }

    // If target task has no test files (null or empty), we still need to delete old test files
    val targetTestFiles: Collection<TaskFile> = cachedTargetTestFiles?.values ?: emptyList()
    val targetTestFileNames = targetTestFiles.map { it.name }.toSet()

    // Delete test files from current task that are not in target task
    val testFilesToDelete = currentTestFileNames - targetTestFileNames
    if (testFilesToDelete.isNotEmpty()) {
      LOG.info("Deleting ${testFilesToDelete.size} old test files: $testFilesToDelete")
      invokeAndWaitIfNeeded {
        runWriteAction {
          // Collect parent directories that may become empty after file deletion
          val potentiallyEmptyDirs = mutableSetOf<VirtualFile>()
          for (fileName in testFilesToDelete) {
            try {
              val file = taskDir.findFileByRelativePath(fileName)
              if (file != null) {
                file.parent?.let { parent ->
                  if (parent != taskDir) {
                    potentiallyEmptyDirs.add(parent)
                  }
                }
                file.delete(this)
              }
            }
            catch (e: Exception) {
              LOG.warn("Failed to delete old test file $fileName", e)
            }
          }
          // Delete empty parent directories (in reverse depth order to handle nested dirs)
          val sortedDirs = potentiallyEmptyDirs.sortedByDescending { it.path.count { c -> c == '/' } }
          for (dir in sortedDirs) {
            try {
              if (dir.isValid && dir.children.isEmpty()) {
                LOG.info("Deleting empty directory: ${dir.name}")
                dir.delete(this)
              }
            }
            catch (e: Exception) {
              LOG.warn("Failed to delete empty directory ${dir.name}", e)
            }
          }
        }
      }
    }

    // Create new test files if target task has any
    if (targetTestFiles.isNotEmpty()) {
      // Log test files info for diagnostics (ALT-10961)
      val filesInfo = targetTestFiles.joinToString { "${it.name}:${it.contents.textualRepresentation.hashCode()}" }
      LOG.info("Recreating ${targetTestFiles.size} test files for task '${targetTask.name}' (step ${targetTask.id}): [$filesInfo]")

      for (taskFile in targetTestFiles) {
        try {
          // createChildFile handles write action internally via runInWriteActionAndWait
          val createdFile = GeneratorUtils.createChildFile(
            project.toCourseInfoHolder(),
            taskDir,
            taskFile.name,
            taskFile.contents,
            taskFile.isEditable
          )
          if (createdFile == null) {
            LOG.error("Failed to create test file ${taskFile.name} - createChildFile returned null")
          }
          else {
            LOG.info("Successfully created test file: ${createdFile.path}")
          }
        }
        catch (e: Exception) {
          LOG.error("Exception while recreating test file ${taskFile.name} for task ${targetTask.name}", e)
        }
      }
    }
  }

  /**
   * Returns [Change]s to propagate user changes from [currentState] to [targetTask].
   *
   * In case, when it's impossible due to simultaneous incompatible user changes in [currentState] and [targetState],
   * it asks user to choose what change he wants to apply.
   */
  private fun calculatePropagationChanges(
    targetTask: Task,
    currentTask: Task,
    currentState: Map<String, String>,
    targetState: Map<String, String>,
    showDialogIfConflict: Boolean
  ): UserChanges {
    val (currentPropagatableFilesState, currentNonPropagatableFilesState) = currentState.split(currentTask)
    val (targetPropagatableFilesState, targetNonPropagatableFilesState) = targetState.split(targetTask)

    // A lesson may have files that were non-propagatable in the previous step, but become propagatable in the new one.
    // We allow files to change a propagation flag from false to true (from non-propagatable to propagatable).
    // Course creators often have such use-case:
    // They want to make some files invisible on some prefix of the task list, and then have them visible in the rest of the tasks
    // So that they will be shown to students and will participate in solving the problem
    // For more detailed explanation, see the documentation:
    // https://jetbrains.team/p/edu/repositories/internal-documentation/files/subsystems/Framework%20Lessons/internal-part-ru.md

    // Calculate files that change propagation flag
    // Note: We also check currentTask.taskFiles directly because invisible files are not included
    // in currentNonPropagatableFilesState (since allFiles only returns visible files)
    // We check if file is invisible AND not in a test directory (test files are handled by recreateTestFiles)
    val taskTestDirs = currentTask.testDirs
    val fromNonPropagatableToPropagatableFilesState = targetPropagatableFilesState.filter { (path, _) ->
      path in currentNonPropagatableFilesState ||
      (currentTask.taskFiles[path]?.isVisible == false &&
       taskTestDirs.none { testDir -> path.startsWith("$testDir/") || path == testDir })
    }
    val fromPropagatableToNonPropagatableFilesState = targetNonPropagatableFilesState.filter { it.key in currentPropagatableFilesState }

    // We assume that files could not change a propagation flag from true to false (for example, from visible to invisible)
    // This behavior is not intended
    if (fromPropagatableToNonPropagatableFilesState.isNotEmpty()) {
      LOG.error("Propagation flag change from propagatable to non-propagatable during navigation in non-template-based lessons is not supported")
    }

    // Only files that do not change a propagation flag can participate in propagating user changes.
    val newCurrentPropagatableFilesState = currentPropagatableFilesState.filter { it.key !in targetNonPropagatableFilesState }
    val newTargetPropagatableFilesState = targetPropagatableFilesState.filter { it.key !in fromNonPropagatableToPropagatableFilesState }

    // Files that change propagation flag are processed separately:
    // (Non-Propagatable -> Propagatable) - Changes for them are not propagated
    // (Propagatable -> Non-Propagatable) - We assume that there are no such files

    // Creates Changes to propagate all current changes of task files to a target task.
    // During propagation, we assume that in the not-template-based framework lessons, all the initial files are the same for each task.
    // Therefore, we will only add user-created files and remove user-deleted files.
    // During propagation, we do not change the text of the files.
    fun calculateCurrentTaskChanges(): UserChanges {
      val toRemove = HashMap(newTargetPropagatableFilesState)
      val propagatableFileChanges = mutableListOf<Change>()

      for ((path, text) in newCurrentPropagatableFilesState) {
        val targetText = toRemove.remove(path)
        // Propagate user-created files
        if (targetText == null) {
          propagatableFileChanges += Change.PropagateLearnerCreatedTaskFile(path, text)
        }
      }

      // Remove user-deleted files
      for ((path, _) in toRemove) {
        propagatableFileChanges += Change.RemoveTaskFile(path)
      }

      // Calculate diff for invisible files and files that become visible and change them without propagation
      val nonPropagatableFileChanges = calculateChanges(
        currentNonPropagatableFilesState,
        targetNonPropagatableFilesState + fromNonPropagatableToPropagatableFilesState
      )
      return nonPropagatableFileChanges + propagatableFileChanges
    }

    // target task initialization
    if (targetTask.record == -1) {
      return calculateCurrentTaskChanges()
    }

    // if current and target states of propagatable files are the same
    // it needs to calculate only diff for non-propagatable files and for files that change the propagation flag from false to true
    if (newCurrentPropagatableFilesState == newTargetPropagatableFilesState) {
      return calculateChanges(
        currentNonPropagatableFilesState,
        targetNonPropagatableFilesState + fromNonPropagatableToPropagatableFilesState
      )
    }

    val keepConflictingChanges = if (showDialogIfConflict) {
      val currentTaskName = "${currentTask.getUIName()} ${currentTask.index}"
      val targetTaskName = "${targetTask.getUIName()} ${targetTask.index}"
      val message = EduCoreBundle.message(
        "framework.lesson.changes.conflict.message", currentTaskName, targetTaskName, targetTaskName,
        currentTaskName
      )
      Messages.showYesNoDialog(
        project,
        message,
        EduCoreBundle.message("framework.lesson.changes.conflicting.changes.title"),
        EduCoreBundle.message("framework.lesson.changes.conflicting.changes.keep"),
        EduCoreBundle.message("framework.lesson.changes.conflicting.changes.replace"),
        null
      )
    }
    else {
      Messages.YES
    }

    return if (keepConflictingChanges == Messages.YES) {
      calculateChanges(currentState, targetState)
    }
    else {
      calculateCurrentTaskChanges()
    }
  }

  private fun getUserChangesFromFiles(initialState: FLTaskState, taskDir: VirtualFile): UserChanges {
    val currentState = getTaskStateFromFiles(initialState.keys, taskDir)
    return calculateChanges(initialState, currentState)
  }

  @Synchronized
  private fun saveSnapshot(
    refId: Int,
    state: Map<String, String>,
    parentRefId: Int = -1,
    initialState: Map<String, String> = emptyMap()
  ): UpdatedUserChanges {
    return try {
      val newRefId = storage.saveSnapshot(refId, state, parentRefId)
      storage.force()
      // Calculate changes on-the-fly by comparing initial state with saved snapshot
      val changes = SnapshotDiff.calculateChanges(initialState, state)
      // Notify listeners about storage change
      val commitHash = storage.resolveRef(newRefId)
      if (commitHash != null) {
        project.messageBus.syncPublisher(FrameworkStorageListener.TOPIC).snapshotSaved(newRefId, commitHash)
      }
      UpdatedUserChanges(newRefId, changes)
    }
    catch (e: IOException) {
      if (e.message?.contains("Corrupted data") == true) {
        LOG.warn("Corrupted storage data detected during snapshot write, recreating storage: ${e.message}")
        recreateStorage()
        return try {
          val newRefId = storage.saveSnapshot(-1, state, parentRefId)
          storage.force()
          val changes = SnapshotDiff.calculateChanges(initialState, state)
          UpdatedUserChanges(newRefId, changes)
        }
        catch (retryError: IOException) {
          LOG.error("Failed to save snapshot after storage recreation", retryError)
          UpdatedUserChanges(-1, UserChanges.empty())
        }
      }
      LOG.error("Failed to save snapshot", e)
      UpdatedUserChanges(refId, UserChanges.empty())
    }
  }

  /**
   * Get user changes from storage by loading snapshot and calculating diff on-the-fly.
   * This is the git-like approach: we store full snapshots and compute diffs when needed.
   */
  private fun getUserChangesFromStorage(task: Task): UserChanges {
    if (task.record == -1) return UserChanges.empty()

    // Get snapshot from storage
    val snapshot = try {
      storage.getSnapshot(task.record)
    }
    catch (e: IOException) {
      if (e.message?.contains("Corrupted data") == true) {
        LOG.warn("Corrupted storage data detected, recreating storage: ${e.message}")
        recreateStorage()
      }
      else {
        LOG.error("Failed to get snapshot for task `${task.name}`", e)
      }
      return UserChanges.empty()
    }
    catch (e: NegativeArraySizeException) {
      LOG.warn("Corrupted storage data detected (negative array size: ${e.message}), recreating storage")
      recreateStorage()
      return UserChanges.empty()
    }

    // Get initial state (templates) from cache or task.allFiles
    val initialState = originalTemplateFilesCache[task.id] ?: task.allFiles

    // Calculate diff: what changes are needed to go from initialState to snapshot
    val changes = SnapshotDiff.calculateChanges(initialState, snapshot)

    // Filter out test file changes (they shouldn't be in storage, but filter just in case)
    val taskTestDirs = task.testDirs
    val filteredChanges = changes.changes.filter { change ->
      val path = change.path
      val isInTestDir = taskTestDirs.any { testDir -> path.startsWith("$testDir/") || path == testDir }
      val fileName = path.substringAfterLast('/')
      val isTestFileName = fileName == "tests.py" || fileName.startsWith("test_") || fileName.endsWith("_test.py")
      val isTestFile = isInTestDir || isTestFileName
      if (isTestFile) {
        LOG.warn("Filtering out test file from snapshot: ${change.javaClass.simpleName}($path)")
      }
      !isTestFile
    }

    if (filteredChanges.size != changes.changes.size) {
      LOG.warn("Filtered ${changes.changes.size - filteredChanges.size} test files from snapshot for task '${task.name}'")
    }

    // Get timestamp from storage
    val timestamp = try {
      storage.getSnapshotTimestamp(task.record)
    }
    catch (e: IOException) {
      0L
    }

    return UserChanges(filteredChanges, timestamp)
  }

  private fun recreateStorage() {
    try {
      Disposer.dispose(storage)
      val storageFilePath = constructStoragePath(project)
      FrameworkStorage.deleteFiles(storageFilePath)
      resetAllTaskRecords(project)
      storage = createStorage(project)

      LOG.warn("Storage recreated successfully after corruption")
    }
    catch (recreateError: Exception) {
      LOG.error("Failed to recreate storage after corruption", recreateError)
    }
  }

  override fun storeOriginalTestFiles(task: Task) {
    // ALT-10961: Don't overwrite cache if it already contains data loaded from API.
    // This is important because task.taskFiles may contain stale data from disk
    // (in framework lessons, all stages share the same task directory).
    // Data loaded from API via loadTestFilesFromApi() is always correct.
    if (originalTestFilesCache.containsKey(task.id)) {
      LOG.info("Cache already contains test files for task '${task.name}' (step ${task.id}), not overwriting")
      return
    }
    storeTestFilesInternal(task)
  }

  override fun updateOriginalTestFiles(task: Task) {
    // Force update the cache, used when task files are updated from remote server
    // (e.g., during course update). Unlike storeOriginalTestFiles, this WILL overwrite.
    LOG.info("Force updating test files cache for task '${task.name}' (step ${task.id})")
    storeTestFilesInternal(task)
  }

  private fun storeTestFilesInternal(task: Task) {
    val testFiles = task.taskFiles.filterValues { taskFile ->
      !taskFile.isLearnerCreated && taskFile.isTestFile
    }
    if (testFiles.isNotEmpty()) {
      // Create copies of TaskFile objects to prevent modification when original task.taskFiles changes
      // This is important because task.taskFiles contents can be updated (e.g., during Update Course)
      // and we want to preserve the original test file contents
      // Force isVisible = false for test files so they don't appear in Project View
      val copiedTestFiles = testFiles.mapValues { (_, taskFile) ->
        TaskFile(taskFile.name, taskFile.contents).also {
          it.isVisible = false
          it.isEditable = taskFile.isEditable
          it.isLearnerCreated = taskFile.isLearnerCreated
        }
      }
      originalTestFilesCache[task.id] = copiedTestFiles
      val filesInfo = copiedTestFiles.entries.joinToString { (name, file) ->
        "$name:size=${file.contents.textualRepresentation.length}"
      }
      LOG.info("Stored ${copiedTestFiles.size} original test files for task '${task.name}' (step ${task.id}): [$filesInfo]")
    }
  }

  override fun getOriginalTestFiles(task: Task): Collection<TaskFile>? {
    return originalTestFilesCache[task.id]?.values
  }

  override fun ensureTestFilesCached(task: Task): Boolean {
    if (originalTestFilesCache.containsKey(task.id)) {
      return true
    }
    // Cache is empty, try to load from API
    return loadTestFilesFromApi(task) != null
  }

  /**
   * Stores original template files (visible non-test files) for the task.
   * These templates are used for calculating user changes correctly.
   *
   * IMPORTANT: Call this when task files are loaded from API (fresh data).
   * Do NOT call after user may have modified files, as TaskFile.contents may be stale.
   */
  override fun storeOriginalTemplateFiles(task: Task) {
    // Don't overwrite cache if it already contains data
    if (originalTemplateFilesCache.containsKey(task.id)) {
      LOG.info("Template cache already contains files for task '${task.name}' (step ${task.id}), not overwriting")
      return
    }

    LOG.warn("storeOriginalTemplateFiles: task='${task.name}', taskFiles=${task.taskFiles.keys}")
    task.taskFiles.forEach { (name, taskFile) ->
      LOG.warn("storeOriginalTemplateFiles: file='$name', isVisible=${taskFile.isVisible}, isTestFile=${taskFile.isTestFile}")
    }

    val templateFiles = task.taskFiles.filterValues { taskFile ->
      taskFile.isVisible && !taskFile.isTestFile
    }
    LOG.warn("storeOriginalTemplateFiles: filtered templateFiles=${templateFiles.keys}")

    if (templateFiles.isNotEmpty()) {
      // Store only the content (as String), not the TaskFile objects
      val cachedTemplates = templateFiles.mapValues { (_, taskFile) ->
        taskFile.contents.textualRepresentation
      }
      originalTemplateFilesCache[task.id] = cachedTemplates
      val filesInfo = cachedTemplates.entries.joinToString { (name, content) ->
        "$name:size=${content.length}"
      }
      LOG.info("Stored ${cachedTemplates.size} original template files for task '${task.name}' (step ${task.id}): [$filesInfo]")

      // ALT-10961: If task has no record yet, save these templates as the initial snapshot in storage.
      // This ensures we have a base state even after IDE restart/offline.
      if (task.record == -1) {
        LOG.info("Task '${task.name}' has no record, saving templates as initial snapshot")
        saveExternalChanges(task, cachedTemplates)
      }
    }
  }

  /**
   * Loads template files from API and caches them.
   * This is used as a fallback when the cache is empty.
   *
   * IMPORTANT: This method must NOT block EDT. If called from EDT, it starts async loading
   * and returns null immediately. The caller should use task.taskFiles as fallback.
   */
  private fun loadTemplateFilesFromApi(task: Task): Map<String, String>? {
    val stepId = task.id
    if (stepId <= 0) {
      LOG.warn("Cannot load template files from API: task '${task.name}' has invalid step ID: $stepId")
      return null
    }

    // Don't block EDT - start async loading and return null
    if (ApplicationManager.getApplication().isDispatchThread) {
      LOG.info("On EDT, starting async load for template files of task '${task.name}' (step $stepId)")
      ApplicationManager.getApplication().executeOnPooledThread {
        loadTemplateFilesFromApiSync(task)
      }
      return null
    }

    return loadTemplateFilesFromApiSync(task)
  }

  /**
   * Synchronously loads template files from API. Must NOT be called from EDT.
   */
  private fun loadTemplateFilesFromApiSync(task: Task): Map<String, String>? {
    val stepId = task.id
    LOG.info("Loading template files from API for task '${task.name}' (step $stepId)")

    return try {
      val stepSourceResult = HyperskillConnector.getInstance().getStepSourceAnonymous(stepId)
      if (stepSourceResult is Err) {
        LOG.warn("Failed to load step source from API for step $stepId: ${stepSourceResult.error}")
        return null
      }

      val stepSource = (stepSourceResult as Ok).value
      val options = stepSource.block?.options as? PyCharmStepOptions
      if (options == null) {
        LOG.warn("Step $stepId has no PyCharmStepOptions")
        return null
      }

      val allFiles = options.files
      if (allFiles.isNullOrEmpty()) {
        LOG.warn("Step $stepId has no files in options")
        return null
      }

      // Filter visible non-test files
      val templateFiles = allFiles.filter { taskFile ->
        taskFile.isVisible && !taskFile.isLearnerCreated && "test" !in taskFile.name.lowercase()
      }

      if (templateFiles.isEmpty()) {
        LOG.warn("Step $stepId has no template files")
        return null
      }

      val cachedTemplates = templateFiles.associate { taskFile ->
        taskFile.name to taskFile.contents.textualRepresentation
      }

      originalTemplateFilesCache[stepId] = cachedTemplates
      val filesInfo = cachedTemplates.entries.joinToString { (name, content) ->
        "$name:size=${content.length}"
      }
      LOG.info("Loaded and cached ${cachedTemplates.size} template files from API for task '${task.name}' (step $stepId): [$filesInfo]")

      cachedTemplates
    }
    catch (e: Exception) {
      LOG.warn("Exception while loading template files from API for step $stepId", e)
      null
    }
  }

  /**
   * Ensures template files are cached for the task.
   * If not cached, attempts to load from API.
   */
  override fun ensureTemplateFilesCached(task: Task): Boolean {
    if (originalTemplateFilesCache.containsKey(task.id)) {
      return true
    }
    return loadTemplateFilesFromApi(task) != null
  }

  override fun dispose() {
    Disposer.dispose(storage)
    originalTestFilesCache.clear()
    originalTemplateFilesCache.clear()
  }

  /**
   * Returns the state of all non-test files for this task.
   * This includes both template files (visible, editable) and learner-created files.
   *
   * Test files are excluded because they are handled separately by [recreateTestFiles]
   * using cached data from API to ensure correct test content for each stage.
   *
   * IMPORTANT: This must return ALL non-test files (not just learner-created) because
   * the framework storage tracks DIFFS from template state to user state. If we only
   * returned learner-created files, the diff calculation would be incorrect.
   */
  private val Task.allFiles: FLTaskState
    get() = taskFiles
      .filterValues { !it.isTestFile } // Exclude test files - they are handled by recreateTestFiles
      .mapValues { it.value.contents.textualRepresentation }

  private fun FLTaskState.splitByKey(predicate: (String) -> Boolean): Pair<FLTaskState, FLTaskState> {
    val positive = HashMap<String, String>()
    val negative = HashMap<String, String>()

    for ((path, text) in this) {
      val state = if (predicate(path)) positive else negative
      state[path] = text
    }

    return positive to negative
  }

  private fun FLTaskState.split(task: Task) = splitByKey { path ->
    val taskFile = task.taskFiles[path]
    // ALT-10961: Also filter out test files by path pattern, not just by taskFile properties.
    // This handles the case where test files exist in externalState (submission) but not in task.taskFiles.
    val taskTestDirs = task.testDirs
    // Check if file is in a test directory
    val isInTestDir = taskTestDirs.any { testDir -> path.startsWith("$testDir/") || path == testDir }
    // Also check for common test file patterns (tests.py, test_*.py, *_test.py)
    val fileName = path.substringAfterLast('/')
    val isTestFileName = fileName == "tests.py" || fileName.startsWith("test_") || fileName.endsWith("_test.py")
    val isTestFilePath = isInTestDir || isTestFileName
    if (isTestFilePath) {
      LOG.info("split: path='$path' excluded as test file")
      return@splitByKey false
    }
    val result = taskFile?.shouldBePropagated() ?: true
    if (!result) {
      LOG.warn("split: path='$path' excluded, isVisible=${taskFile.isVisible}, isEditable=${taskFile.isEditable}")
    }
    result
  }

  @TestOnly
  override fun restoreState() {
    if (storage.isDisposed) {
      storage = createStorage(project)
    }
  }

  @TestOnly
  override fun cleanUpState() {
    storage.closeAndClean()
    originalTestFilesCache.clear()
    originalTemplateFilesCache.clear()
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(FrameworkLessonManagerImpl::class.java)

    const val VERSION: Int = 3

    @VisibleForTesting
    fun constructStoragePath(project: Project): Path =
      Paths.get(FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistory", "storage"))

    private fun createStorage(project: Project): FrameworkStorage {
      val storageFilePath = constructStoragePath(project)
      val storageExists = storageFilePath.toFile().exists()
      LOG.warn("CREATE_STORAGE: path=$storageFilePath, exists=$storageExists")

      try {
        val storage = FrameworkStorage(storageFilePath)
        storage.migrate(VERSION)
        LOG.warn("CREATE_STORAGE: success, version=${storage.version}")
        return storage
      }
      catch (e: Exception) {
        LOG.error("Failed to initialize storage at $storageFilePath, recreating", e)
        try {
          FrameworkStorage.deleteFiles(storageFilePath)
        }
        catch (deleteError: Exception) {
          LOG.error("Failed to delete old storage files", deleteError)
        }
        resetAllTaskRecords(project)
        return FrameworkStorage(storageFilePath, VERSION)
      }
    }

    private fun resetAllTaskRecords(project: Project) {
      val course = StudyTaskManager.getInstance(project).course ?: return
      for (lesson in course.lessons) {
        if (lesson !is FrameworkLesson) continue
        for (task in lesson.taskList) {
          if (task.record != -1) {
            LOG.info("Resetting task record for '${task.name}' from ${task.record} to -1")
            task.record = -1
          }
        }
        YamlFormatSynchronizer.saveItem(lesson)
      }
    }
  }
}

private data class UpdatedUserChanges(
  val refId: Int,
  val changes: UserChanges
)
