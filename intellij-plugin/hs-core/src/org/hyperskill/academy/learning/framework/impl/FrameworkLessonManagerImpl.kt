package org.hyperskill.academy.learning.framework.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
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
 * Extension to get storage ref for a task.
 * @see getStorageRef
 */
private fun Task.storageRef(): String = getStorageRef()

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

  // Flag to track user's propagation choice during multi-stage navigation (jump).
  // - null: no choice made yet (first stage in jump) → show dialog
  // - true: user chose Replace → show dialog for each subsequent stage
  // - false: user chose Keep → auto-Keep for all subsequent stages without dialog
  private var propagationActive: Boolean? = null

  // Flag to skip auto-save during navigation (prevents auto-save from creating commits
  // with "Auto-save" message when navigation code should be creating commits)
  private var isNavigating = false

  // Flag to track if currentTaskIndex has been synced with storage HEAD.
  // Auto-save is skipped until sync is done to prevent saving wrong stage content.
  private var isStorageSynced = false

  init {
    // Subscribe to file save events to persist changes to storage
    val connection = project.messageBus.connect(this)
    connection.subscribe(FileDocumentManagerListener.TOPIC, object : FileDocumentManagerListener {
      override fun beforeAllDocumentsSaving() {
        // Called when user saves all documents (Ctrl+S) or before IDE closes
        saveCurrentTaskSnapshot()
      }
    })
  }

  override fun prepareNextTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean) {
    applyTargetTaskChanges(lesson, 1, taskDir, showDialogIfConflict)
  }

  override fun preparePrevTask(lesson: FrameworkLesson, taskDir: VirtualFile, showDialogIfConflict: Boolean) {
    applyTargetTaskChanges(lesson, -1, taskDir, showDialogIfConflict)
  }

  /**
   * Get the parent task's storage ref.
   * For a task at index N, the parent is the task at index N-1.
   * Returns null if there's no parent (first task in lesson).
   */
  private fun getParentRef(task: Task): String? {
    val lesson = task.lesson as? FrameworkLesson ?: return null
    val taskIndex = task.index
    return if (taskIndex > 1) {
      lesson.taskList.getOrNull(taskIndex - 2)?.storageRef()
    } else null
  }

  /**
   * Check if a task has data in storage.
   */
  private fun hasStorageData(task: Task): Boolean {
    return storage.hasRef(task.storageRef())
  }

  override fun saveExternalChanges(task: Task, externalState: Map<String, String>) {
    require(task.lesson is FrameworkLesson) {
      "Only solutions of framework tasks can be saved"
    }

    val ref = task.storageRef()
    val parentRef = getParentRef(task)
    LOG.warn("saveExternalChanges: task='${task.name}', ref=$ref, externalState.keys=${externalState.keys}")

    // Filter external state to only include propagatable files (exclude test files, etc.)
    val externalPropagatableFiles = externalState.split(task).first
    LOG.warn("saveExternalChanges: externalPropagatableFiles.keys=${externalPropagatableFiles.keys}")

    // Save the API state as a snapshot
    val message = "Load submission from server for '${task.name}'"
    try {
      storage.saveSnapshot(ref, externalPropagatableFiles, parentRef, message)
    }
    catch (e: IOException) {
      LOG.error("Failed to save solution for task `${task.name}`", e)
    }

    // Migration: If legacy storage has changes for this task's old refId, apply them on top.
    // This preserves user's local changes that weren't submitted to the server.
    val legacyRefId = task.record
    if (legacyRefId != -1 && storage.hasLegacyChanges(legacyRefId)) {
      LOG.info("Migrating legacy changes for task '${task.name}' (legacyRefId=$legacyRefId)")
      try {
        storage.applyLegacyChangesAndSave(ref, legacyRefId, externalPropagatableFiles, parentRef)
      }
      catch (e: IOException) {
        LOG.error("Failed to apply legacy changes for task `${task.name}`", e)
      }
    }

    // Clear legacy record since we now use computed refs
    if (task.record != -1) {
      task.record = -1
      YamlFormatSynchronizer.saveItem(task)
    }
    LOG.warn("saveExternalChanges: task='${task.name}', saved to ref=$ref, parentRef=$parentRef")
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

    val ref = task.storageRef()
    return try {
      storage.getSnapshotTimestamp(ref)
    }
    catch (e: IOException) {
      LOG.warn("Failed to get snapshot timestamp for task `${task.name}` (ref=$ref)", e)
      0L
    }
  }

  override fun getTaskState(lesson: FrameworkLesson, task: Task): Map<String, String> {
    require(task.lesson == lesson) {
      "The task is not a part of this lesson"
    }

    // For current task, read from disk
    if (lesson.currentTaskIndex + 1 == task.index) {
      val taskDir = task.getDir(project.courseDir) ?: return emptyMap()
      val initialFiles = task.allFiles
      val changes = getUserChangesFromFiles(initialFiles, taskDir)
      return HashMap(initialFiles).apply { changes.apply(this) }
    }

    // For other tasks, read snapshot directly from storage
    val ref = task.storageRef()
    return if (storage.hasRef(ref)) {
      try {
        storage.getSnapshot(ref)
      } catch (e: IOException) {
        LOG.warn("Failed to get snapshot for task '${task.name}' (ref=$ref), falling back to templates", e)
        task.allFiles
      }
    } else {
      task.allFiles
    }
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

    // Set navigation flag to prevent auto-save from creating commits during navigation
    // Use try-finally to guarantee the flag is reset even if an exception occurs
    isNavigating = true
    try {
      applyTargetTaskChangesImpl(lesson, currentTask, targetTask, currentTaskIndex, targetTaskIndex, taskIndexDelta, taskDir, showDialogIfConflict)
    }
    finally {
      isNavigating = false
      // After navigation, currentTaskIndex is correct - enable auto-save
      isStorageSynced = true
    }

    LOG.info("=== Stage switch complete ===")
  }

  /**
   * Implementation of applyTargetTaskChanges, extracted to allow try-finally wrapper.
   */
  private fun applyTargetTaskChangesImpl(
    lesson: FrameworkLesson,
    currentTask: Task,
    targetTask: Task,
    currentTaskIndex: Int,
    targetTaskIndex: Int,
    taskIndexDelta: Int,
    taskDir: VirtualFile,
    showDialogIfConflict: Boolean
  ) {
    // Reset propagation flag on backward navigation
    if (taskIndexDelta < 0) {
      propagationActive = null
    }

    lesson.currentTaskIndex = targetTaskIndex
    SlowOperations.knownIssue("EDU-XXXX").use {
      YamlFormatSynchronizer.saveItem(lesson)
    }

    val currentRef = currentTask.storageRef()
    val targetRef = targetTask.storageRef()
    val currentHasStorage = hasStorageData(currentTask)
    val targetHasStorage = hasStorageData(targetTask)

    LOG.info("Navigation refs: current=$currentRef (hasStorage=$currentHasStorage), target=$targetRef (hasStorage=$targetHasStorage)")

    // 1. Get current disk state (what's currently on disk)
    // Use template file keys to know which files to read
    val templateFiles = originalTemplateFilesCache[currentTask.id] ?: currentTask.allFiles
    val currentDiskState = getTaskStateFromFiles(templateFiles.keys, taskDir)
    val (currentPropagatableFiles, _) = currentDiskState.split(currentTask)

    // 2. Save current state to storage ONLY when navigating FORWARD.
    // When navigating backward, the disk content belongs to the stage we're leaving,
    // not the stage we're going from. Saving it would corrupt the current stage's snapshot.
    if (taskIndexDelta > 0) {
      val navMessage = "Save changes before navigating from '${currentTask.name}' to '${targetTask.name}'"
      try {
        storage.saveSnapshot(currentRef, currentPropagatableFiles, getParentRef(currentTask), navMessage)
        LOG.info("Saved snapshot for current task '${currentTask.name}' (ref=$currentRef)")
      }
      catch (e: IOException) {
        LOG.error("Failed to save snapshot for task `${currentTask.name}`", e)
      }
    } else {
      LOG.info("Navigation: Moving backward, not saving current task '${currentTask.name}' (would corrupt snapshot)")
    }

    // 3. Clear legacy record if present (we now use computed refs)
    if (currentTask.record != -1) {
      currentTask.record = -1
      SlowOperations.knownIssue("EDU-XXXX").use {
        YamlFormatSynchronizer.saveItem(currentTask)
      }
    }

    // 4. Get current state for diff calculation
    // For forward navigation: use disk state (we just saved it)
    // For backward navigation: use disk state (what's currently there)
    val currentState: FLTaskState = currentPropagatableFiles
    LOG.warn("Navigation: currentState=${currentState.mapValues { "${it.key}:${it.value.length}chars" }}")

    // 5. Get target state directly from storage snapshot (no template-based diff calculation needed)
    // This is simpler and more reliable than calculating diffs from templates.
    val targetState: FLTaskState = if (targetHasStorage) {
      try {
        storage.getSnapshot(targetRef)
      } catch (e: IOException) {
        LOG.error("Failed to get snapshot for target task '${targetTask.name}' (ref=$targetRef), falling back to templates", e)
        targetTask.allFiles
      }
    } else {
      // No storage data for target - use template files
      targetTask.allFiles
    }
    LOG.warn("Navigation: targetState=${targetState.mapValues { "${it.key}:${it.value.length}chars" }}, fromStorage=$targetHasStorage")

    // 6. Calculate difference between latest states of current and target tasks
    // Note, there are special rules for hyperskill courses for now
    // All user changes from the current task should be propagated to next task as is
    //
    // Check if merge is needed using git-like ancestor check:
    // - If current commit is ancestor of target commit → no merge needed (changes already propagated)
    // - If current commit is NOT ancestor of target commit → need merge, show Keep/Replace dialog
    val currentCommitIsAncestorOfTarget = targetHasStorage && storage.isAncestor(currentRef, targetRef)
    val needsMerge = !currentCommitIsAncestorOfTarget && targetHasStorage && taskIndexDelta == 1 && lesson.propagateFilesOnNavigation
    LOG.info("Merge check: currentRef=$currentRef, targetRef=$targetRef, isAncestor=$currentCommitIsAncestorOfTarget, needsMerge=$needsMerge")

    // Track if merge commit was created (to skip redundant snapshot save in step 10)
    var mergeCommitCreated = false

    val changes = if (needsMerge) {
      mergeCommitCreated = true // Merge commit will be created in calculatePropagationChanges
      calculatePropagationChanges(targetTask, currentTask, currentState, targetState, showDialogIfConflict, targetHasStorage, currentRef, targetRef)
    }
    else {
      propagationActive = null // No propagation happening, reset for next navigation
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

    // Clear legacy record for target task if present
    if (targetTask.record != -1) {
      targetTask.record = -1
      SlowOperations.knownIssue("EDU-XXXX").use {
        YamlFormatSynchronizer.saveItem(targetTask)
      }
    }

    // 10. Save snapshot for target stage after forward navigation.
    // Skip if merge commit was already created (to avoid redundant commits).
    // Only save for:
    // - Target without storage (first visit to this stage)
    // - Navigation without merge (ancestor check passed, no Keep/Replace dialog)
    if (taskIndexDelta > 0 && !mergeCommitCreated) {
      val finalDiskState = getTaskStateFromFiles(targetState.keys, taskDir)
      val (finalPropagatableFiles, _) = finalDiskState.split(targetTask)
      val message = "Navigate to '${targetTask.name}'"
      try {
        storage.saveSnapshot(targetRef, finalPropagatableFiles, getParentRef(targetTask), message)
        LOG.info("Saved snapshot for target task '${targetTask.name}' (ref=$targetRef)")
      }
      catch (e: IOException) {
        LOG.error("Failed to save snapshot for target task '${targetTask.name}'", e)
      }
    }

    // Update HEAD to point to the current (target) task
    storage.head = targetRef
    LOG.info("HEAD updated to ref $targetRef (task '${targetTask.name}')")
    project.messageBus.syncPublisher(FrameworkStorageListener.TOPIC).headUpdated(targetRef)
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
   *
   * Creates merge commits with two parents when user chooses Keep or Replace:
   * - Replace: merge commit with content from current stage (propagate changes)
   * - Keep: merge commit with content from target stage (preserve target's changes)
   *
   * @param targetHasStorage true if target task has saved snapshot in storage
   * @param currentRef ref of the current task (for merge commit parent)
   * @param targetRef ref of the target task (for merge commit parent)
   */
  private fun calculatePropagationChanges(
    targetTask: Task,
    currentTask: Task,
    currentState: Map<String, String>,
    targetState: Map<String, String>,
    showDialogIfConflict: Boolean,
    targetHasStorage: Boolean,
    currentRef: String,
    targetRef: String
  ): UserChanges {
    val (currentPropagatableFilesState, currentNonPropagatableFilesState) = currentState.split(currentTask)
    val (targetPropagatableFilesState, targetNonPropagatableFilesState) = targetState.split(targetTask)

    // If user previously chose Keep in this navigation sequence, auto-Keep without showing dialog
    if (propagationActive == false) {
      LOG.info("Auto-Keep for '$targetRef' (user chose Keep in previous step)")
      val mergeMessage = "Merge from '${currentTask.name}': Keep target changes (auto)"
      try {
        storage.saveMergeSnapshot(targetRef, targetPropagatableFilesState, listOf(targetRef, currentRef), mergeMessage)
        LOG.info("Created auto-Keep merge commit for '$targetRef' with parents [$targetRef, $currentRef]")
      } catch (e: IOException) {
        LOG.error("Failed to create auto-Keep merge commit for '$targetRef'", e)
      }
      return calculateChanges(currentState, targetState)
    }

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

    // Target task has no saved state - propagate changes without asking
    if (!targetHasStorage) {
      return calculateCurrentTaskChanges()
    }

    // If current and target propagatable files are identical - no need for dialog
    // Just calculate diff for non-propagatable files
    if (newCurrentPropagatableFilesState == newTargetPropagatableFilesState) {
      return calculateChanges(
        currentNonPropagatableFilesState,
        targetNonPropagatableFilesState + fromNonPropagatableToPropagatableFilesState
      )
    }

    // Target has saved state AND content differs - ask user before overwriting
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
      // When dialog is suppressed, default to Keep (preserve target's content).
      // This is the safest default for automated scenarios (e.g., lesson updates).
      // For project open, the caller should pass showDialogIfConflict=true to let user decide.
      LOG.info("Dialog suppressed, using Keep to preserve target content for '${targetTask.name}'")
      Messages.YES
    }

    return if (keepConflictingChanges == Messages.YES) {
      // User chose Keep - create merge commit with target's content
      // This records that we "merged" but kept our version (like git merge with "ours" strategy)
      propagationActive = false
      val mergeMessage = "Merge from '${currentTask.name}': Keep target changes"
      try {
        // Parents: [targetRef, currentRef] - we're merging currentRef into targetRef
        storage.saveMergeSnapshot(targetRef, targetPropagatableFilesState, listOf(targetRef, currentRef), mergeMessage)
        LOG.info("Created Keep merge commit for '$targetRef' with parents [$targetRef, $currentRef]")
      } catch (e: IOException) {
        LOG.error("Failed to create Keep merge commit for '$targetRef'", e)
      }
      calculateChanges(currentState, targetState)
    }
    else {
      // User chose Replace - create merge commit with current's content
      // This propagates changes from current stage to target stage
      propagationActive = true
      val mergeMessage = "Merge from '${currentTask.name}': Replace with propagated changes"
      try {
        // Parents: [targetRef, currentRef] - we're merging currentRef into targetRef
        storage.saveMergeSnapshot(targetRef, currentPropagatableFilesState, listOf(targetRef, currentRef), mergeMessage)
        LOG.info("Created Replace merge commit for '$targetRef' with parents [$targetRef, $currentRef]")
      } catch (e: IOException) {
        LOG.error("Failed to create Replace merge commit for '$targetRef'", e)
      }
      calculateCurrentTaskChanges()
    }
  }

  private fun getUserChangesFromFiles(initialState: FLTaskState, taskDir: VirtualFile): UserChanges {
    val currentState = getTaskStateFromFiles(initialState.keys, taskDir)
    return calculateChanges(initialState, currentState)
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

      // ALT-10961: If task has no data in storage yet, save these templates as the initial snapshot.
      // This ensures we have a base state even after IDE restart/offline.
      if (!hasStorageData(task)) {
        LOG.info("Task '${task.name}' has no storage data, saving templates as initial snapshot")
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
   * Saves a snapshot of the current task's state from disk.
   * Called when files are saved (Ctrl+S, auto-save, before IDE closes).
   * This ensures user changes are persisted even without navigation.
   */
  private fun saveCurrentTaskSnapshot() {
    // Skip auto-save during navigation - navigation code handles saving with proper messages
    if (isNavigating) return

    // Skip auto-save until currentTaskIndex is synced with storage HEAD.
    // This prevents saving wrong stage content when project opens and currentTaskIndex is still 0.
    if (!isStorageSynced) {
      LOG.info("saveCurrentTaskSnapshot: skipped - storage not synced yet")
      return
    }

    val course = StudyTaskManager.getInstance(project).course ?: return
    val lesson = course.lessons.filterIsInstance<FrameworkLesson>().firstOrNull() ?: return
    val currentTask = lesson.currentTask() ?: return
    val taskDir = currentTask.getDir(project.courseDir) ?: return

    // Get cached templates or fall back to task.allFiles
    val initialFiles = originalTemplateFilesCache[currentTask.id] ?: currentTask.allFiles
    if (initialFiles.isEmpty()) return

    // Read current disk state
    val currentDiskState = getTaskStateFromFiles(initialFiles.keys, taskDir)
    val (propagatableFiles, _) = currentDiskState.split(currentTask)

    // Check if there are actual changes compared to saved snapshot
    val ref = currentTask.storageRef()
    val existingSnapshot = try {
      if (storage.hasRef(ref)) storage.getSnapshot(ref) else emptyMap()
    } catch (e: IOException) {
      emptyMap()
    }

    if (propagatableFiles == existingSnapshot) {
      // No changes to save
      return
    }

    // Save snapshot
    try {
      val created = storage.saveSnapshot(ref, propagatableFiles, getParentRef(currentTask), "Auto-save changes for '${currentTask.name}'")
      if (created) {
        LOG.info("Auto-saved snapshot for task '${currentTask.name}' (ref=$ref)")
      }
    } catch (e: IOException) {
      LOG.warn("Failed to auto-save snapshot for task '${currentTask.name}'", e)
    }
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

  override fun syncCurrentTaskIndexFromStorage(lesson: FrameworkLesson): Boolean {
    val head = storage.head ?: run {
      // No HEAD means fresh storage - mark as synced since currentTaskIndex=0 is correct
      isStorageSynced = true
      return false
    }

    // Find the task whose storageRef matches HEAD
    val taskIndex = lesson.taskList.indexOfFirst { it.storageRef() == head }
    if (taskIndex == -1) {
      LOG.warn("syncCurrentTaskIndexFromStorage: HEAD=$head but no task found with matching storageRef")
      // Still mark as synced to allow auto-save to work
      isStorageSynced = true
      return false
    }

    if (lesson.currentTaskIndex != taskIndex) {
      LOG.info("syncCurrentTaskIndexFromStorage: syncing currentTaskIndex from ${lesson.currentTaskIndex} to $taskIndex (HEAD=$head)")
      lesson.currentTaskIndex = taskIndex
      SlowOperations.knownIssue("EDU-XXXX").use {
        YamlFormatSynchronizer.saveItem(lesson)
      }
    }

    isStorageSynced = true
    return true
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
