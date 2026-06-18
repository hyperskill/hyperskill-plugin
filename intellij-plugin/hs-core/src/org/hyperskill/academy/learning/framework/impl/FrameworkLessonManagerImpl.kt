package org.hyperskill.academy.learning.framework.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.configuration.excludeFromArchive
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.configurator
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
import org.hyperskill.academy.learning.framework.storage.FileEntry
import org.hyperskill.academy.learning.framework.storage.UserChanges
import org.hyperskill.academy.learning.framework.ui.PropagationConflictDialog
import org.hyperskill.academy.learning.stepik.PyCharmStepOptions
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
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

// PERSISTED - do not rename; stored in framework storage snapshots.
private const val LEARNER_MODIFIED_METADATA_KEY = "learnerModified"

/**
 * Keeps list of [Change]s for each task. Change list is difference between initial task state and latest one.
 *
 * Allows navigating between tasks in framework lessons in learner mode (where only current task is visible for a learner)
 * without rewriting whole task content.
 * It can be essential in large projects like Android applications where a lot of files are the same between two consecutive tasks
 */
class FrameworkLessonManagerImpl(private val project: Project) : FrameworkLessonManager, Disposable {
  private var storage: FrameworkStorage = createStorage(project)

  // Cache of original non-propagatable files (test files + hidden files) from API for each task (by step ID)
  // These are used to recreate files with correct content when navigating between stages
  // Non-propagatable files: isVisible=false OR isEditable=false (i.e., !shouldBePropagated())
  private val originalNonPropagatableFilesCache = java.util.concurrent.ConcurrentHashMap<Int, Map<String, TaskFile>>()

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

  override fun saveExternalChanges(task: Task, externalState: Map<String, String>, submissionId: Long?) {
    require(task.lesson is FrameworkLesson) {
      "Only solutions of framework tasks can be saved"
    }

    val ref = task.storageRef()
    val parentRef = getParentRef(task)
    LOG.debug("saveExternalChanges: task='${task.name}', ref=$ref, submissionId=$submissionId, externalState.keys=${externalState.keys}")

    // Filter external state to only include propagatable files (exclude test files from submission)
    val externalPropagatableFiles = externalState.split(task).first
    LOG.debug("saveExternalChanges: externalPropagatableFiles.keys=${externalPropagatableFiles.keys}")

    // Build full snapshot: user files from submission + non-propagatable files from cache.
    // Submission test files are intentionally ignored; API-provided tests stay authoritative.
    val (templatePropagatableFiles, _) = task.allFiles.split(task)
    val fullSnapshot = buildFullSnapshotState(task, templatePropagatableFiles + externalPropagatableFiles)

    // Save the full snapshot
    val submissionInfo = if (submissionId != null) " (submission #$submissionId)" else ""
    val message = "Load submission from server for '${task.name}'$submissionInfo"
    try {
      storage.saveSnapshot(ref, fullSnapshot, parentRef, message)
      LOG.info("Saved full snapshot for external changes: ${fullSnapshot.size} files")
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
        // Convert to content-only map for legacy migration (metadata will use defaults)
        storage.applyLegacyChangesAndSave(ref, legacyRefId, fullSnapshot.toContentMap(), parentRef)
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
    LOG.debug("saveExternalChanges: task='${task.name}', saved to ref=$ref, parentRef=$parentRef")
  }

  override fun updateUserChanges(task: Task, newInitialState: Map<String, String>, newTaskFiles: Map<String, TaskFile>) {
    require(task.lesson is FrameworkLesson) {
      "Only framework task snapshots can be updated"
    }

    val ref = task.storageRef()
    val oldInitialState = task.allFilesIncludingTests
    val currentSnapshot = try {
      if (storage.hasRef(ref)) storage.getSnapshot(ref) else oldInitialState.toFileEntries(task)
    }
    catch (e: IOException) {
      LOG.warn("Failed to load snapshot for task '${task.name}' before update, using task files", e)
      oldInitialState.toFileEntries(task)
    }

    val updatedSnapshot = linkedMapOf<String, FileEntry>()
    val paths = LinkedHashSet<String>().apply {
      addAll(currentSnapshot.keys)
      addAll(newInitialState.keys)
    }

    val metadataTaskFiles = newTaskFiles.ifEmpty { task.taskFiles }

    for (path in paths) {
      val currentEntry = currentSnapshot[path]
      val oldText = oldInitialState[path]
      val newText = newInitialState[path]

      if (newText == null) {
        if (currentEntry != null && (oldText == null || currentEntry.content != oldText)) {
          updatedSnapshot[path] = currentEntry.withLearnerModification()
        }
        continue
      }

      updatedSnapshot[path] = when {
        currentEntry == null -> resolveFileEntryMetadata(path, newText, task, task.testDirs, metadataTaskFiles)
        oldText == null -> currentEntry.withLearnerModification()
        currentEntry.content == oldText -> resolveFileEntryMetadata(path, newText, task, task.testDirs, metadataTaskFiles)
        else -> currentEntry.withLearnerModification()
      }
    }

    try {
      storage.saveSnapshot(ref, updatedSnapshot, getParentRef(task), "Update initial files for '${task.name}'")
      updateOriginalTemplateFilesCache(task, metadataTaskFiles)
    }
    catch (e: IOException) {
      LOG.error("Failed to update snapshot for task '${task.name}'", e)
    }
  }

  override fun addNewFilesToSnapshot(task: Task, newFiles: Map<String, String>) {
    if (newFiles.isEmpty()) return

    require(task.lesson is FrameworkLesson) {
      "Only framework task snapshots can be updated"
    }

    val ref = task.storageRef()

    // Only update if snapshot exists (user visited this task before)
    if (!storage.hasRef(ref)) {
      LOG.info("addNewFilesToSnapshot: No snapshot exists for task '${task.name}' (ref=$ref), skipping")
      return
    }

    try {
      // Get current snapshot (Map<String, FileEntry>)
      val currentSnapshot = storage.getSnapshot(ref)

      // Add only files that don't exist in the snapshot
      val filesToAdd = newFiles.filterKeys { it !in currentSnapshot }
      if (filesToAdd.isEmpty()) {
        LOG.info("addNewFilesToSnapshot: All new files already exist in snapshot for task '${task.name}'")
        return
      }

      // Convert new files to FileEntry with metadata from task
      val filesToAddWithMetadata = filesToAdd.toFileEntries(task, originalNonPropagatableFilesCache[task.id])

      // Merge: existing snapshot + new files
      val mergedSnapshot = currentSnapshot + filesToAddWithMetadata

      // Save updated snapshot
      val message = "Add ${filesToAdd.size} new template files from server"
      storage.saveSnapshot(ref, mergedSnapshot, null, message)
      LOG.info("addNewFilesToSnapshot: Added ${filesToAdd.size} new files to snapshot for task '${task.name}': ${filesToAdd.keys}")
    }
    catch (e: IOException) {
      LOG.error("Failed to add new files to snapshot for task '${task.name}'", e)
    }
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

    val ref = task.storageRef()
    if (storage.hasRef(ref)) {
      try {
        val snapshotState = storage.getSnapshot(ref).toContentMap()
        if (lesson.currentTaskIndex + 1 == task.index) {
          val taskDir = task.getDir(project.courseDir) ?: return snapshotState
          val diskState = getAllFilesFromTaskDir(taskDir, task)
          // Compare only propagatable (user-editable) files: test/hidden files on disk are
          // stage-specific and absent from the template (task.allFiles), so including them would
          // make this "disk still holds the pristine template" check effectively never match.
          val diskPropagatableFiles = diskState.split(task).first
          val templatePropagatableFiles = task.allFiles.split(task).first
          val snapshotPropagatableFiles = snapshotState.split(task).first
          val diskIsPristineTemplate = diskPropagatableFiles == templatePropagatableFiles
          return if (diskIsPristineTemplate && snapshotPropagatableFiles != diskPropagatableFiles) snapshotState else diskState
        }
        return snapshotState
      } catch (e: IOException) {
        LOG.warn("Failed to get snapshot for task '${task.name}' (ref=$ref), falling back to templates", e)
      }
    }

    // Current task may contain unsaved editor changes; read disk/documents before storage.
    if (lesson.currentTaskIndex + 1 == task.index) {
      val taskDir = task.getDir(project.courseDir) ?: return emptyMap()
      return getAllFilesFromTaskDir(taskDir, task)
    }

    return task.allFiles
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
    val navigationStartTime = System.currentTimeMillis()
    var lastStepTime = navigationStartTime

    fun logTiming(step: String) {
      val now = System.currentTimeMillis()
      val stepDuration = now - lastStepTime
      val totalDuration = now - navigationStartTime
      LOG.info("Navigation timing: $step took ${stepDuration}ms (total: ${totalDuration}ms)")
      lastStepTime = now
    }

    // Reset propagation flag on backward navigation
    if (taskIndexDelta < 0) {
      propagationActive = null
    }

    lesson.currentTaskIndex = targetTaskIndex
    YamlFormatSynchronizer.saveItem(lesson)
    logTiming("saveLesson")

    val currentRef = currentTask.storageRef()
    val targetRef = targetTask.storageRef()
    val currentHasStorage = hasStorageData(currentTask)
    val targetHasStorage = hasStorageData(targetTask)

    LOG.info("Navigation refs: current=$currentRef (hasStorage=$currentHasStorage), target=$targetRef (hasStorage=$targetHasStorage)")

    // 1. Get current disk state (what's currently on disk)
    // Read ALL files from disk, including user-created files
    val currentDiskState = getAllFilesFromTaskDir(taskDir, currentTask)
    val (currentPropagatableFiles, _) = currentDiskState.split(currentTask)
    val currentSnapshotState = if (currentHasStorage) {
      try {
        storage.getSnapshot(currentRef).toContentMap()
      }
      catch (e: IOException) {
        LOG.warn("Failed to get snapshot for current task '${currentTask.name}' (ref=$currentRef), using disk state", e)
        null
      }
    }
    else {
      null
    }
    val currentSnapshotPropagatableFiles = currentSnapshotState?.split(currentTask)?.first
    val (currentTemplatePropagatableFiles, _) = currentTask.allFiles.split(currentTask)
    val useStoredCurrentState = currentSnapshotPropagatableFiles != null &&
      currentPropagatableFiles == currentTemplatePropagatableFiles &&
      currentSnapshotPropagatableFiles != currentPropagatableFiles
    val effectiveCurrentPropagatableFiles = if (useStoredCurrentState) {
      LOG.info("Navigation: using saved snapshot for current task '${currentTask.name}' instead of unchanged template on disk")
      currentSnapshotPropagatableFiles
    }
    else {
      currentPropagatableFiles
    }
    logTiming("readCurrentDiskState")

    // 2. Save current state to storage before leaving the stage.
    if (!useStoredCurrentState) {
      // Build full snapshot: user files from disk + non-propagatable files from cache
      val fullSnapshot = buildFullSnapshotState(currentTask, effectiveCurrentPropagatableFiles)
      logTiming("buildFullSnapshotState(current)")
      val navMessage = "Save changes before navigating from '${currentTask.name}' to '${targetTask.name}'"
      try {
        storage.saveSnapshot(currentRef, fullSnapshot, getParentRef(currentTask), navMessage)
        LOG.info("Saved full snapshot for current task '${currentTask.name}' (ref=$currentRef): ${fullSnapshot.size} files")
      }
      catch (e: IOException) {
        LOG.error("Failed to save snapshot for task `${currentTask.name}`", e)
      }
      logTiming("saveSnapshot(current)")
    }
    else {
      val reason = "saved snapshot is newer than unchanged template on disk"
      LOG.info("Navigation: not saving current task '${currentTask.name}': $reason")
    }

    // 3. Get current state for diff calculation
    // For forward navigation: use disk state (we just saved it)
    // For backward navigation: use disk state (what's currently there)
    val currentState: FLTaskState = effectiveCurrentPropagatableFiles
    LOG.debug("Navigation: currentState=${currentState.mapValues { "${it.key}:${it.value.length}chars" }}")

    // 4. Get target state directly from storage snapshot (no template-based diff calculation needed)
    // This is simpler and more reliable than calculating diffs from templates.
    val targetState: FLTaskState = if (targetHasStorage) {
      try {
        storage.getSnapshot(targetRef).toContentMap().withoutDeletedTemplateFiles(targetTask)
      } catch (e: IOException) {
        LOG.error("Failed to get snapshot for target task '${targetTask.name}' (ref=$targetRef), falling back to templates", e)
        targetTask.allFiles
      }
    } else {
      // No storage data for target - use template files
      targetTask.allFiles
    }
    logTiming("getTargetState")
    LOG.debug("Navigation: targetState=${targetState.mapValues { "${it.key}:${it.value.length}chars" }}, fromStorage=$targetHasStorage")

    // 5. Calculate difference between latest states of current and target tasks
    // Note, there are special rules for hyperskill courses for now
    // All user changes from the current task should be propagated to next task as is
    //
    // Check if merge is needed using git-like ancestor check:
    // - If current commit is ancestor of target commit → no merge needed (changes already propagated)
    // - If current commit is NOT ancestor of target commit → check if propagatable files changed
    // - If only test/hidden files changed (not user code) → auto-Keep merge (record ancestry, keep target's code)
    val currentCommitIsAncestorOfTarget = targetHasStorage && storage.isAncestor(currentRef, targetRef)
    val needsMerge = !currentCommitIsAncestorOfTarget && targetHasStorage && taskIndexDelta == 1 && lesson.propagateFilesOnNavigation
    LOG.info("Merge check: currentRef=$currentRef, targetRef=$targetRef, isAncestor=$currentCommitIsAncestorOfTarget, needsMerge=$needsMerge")

    // Track if merge commit was created (to skip redundant snapshot save in step 10)
    var mergeCommitCreated = false

    val changes = when {
      needsMerge -> {
        mergeCommitCreated = true // Merge commit will be created in calculatePropagationChanges
        val fullCurrentState = buildFullSnapshotState(currentTask, effectiveCurrentPropagatableFiles).toContentMap()
        calculatePropagationChanges(targetTask, currentTask, fullCurrentState, targetState, showDialogIfConflict, targetHasStorage, currentRef, targetRef)
      }
      // First visit to new stage (forward navigation with propagation enabled):
      // Keep all current files and add only NEW files from target templates
      !targetHasStorage && taskIndexDelta > 0 && lesson.propagateFilesOnNavigation -> {
        LOG.info("First visit to '${targetTask.name}': propagating current state")
        calculateFirstVisitChanges(currentState, targetState, currentTask, targetTask)
      }
      else -> {
        propagationActive = null // No propagation happening, reset for next navigation
        calculateChanges(currentState, targetState)
      }
    }
    logTiming("calculateChanges")

    // 6. Apply difference between latest states of current and target tasks on local FS
    val taskFilesChanged = changes.changes.any { it is Change.PropagateLearnerCreatedTaskFile || it is Change.RemoveTaskFile }
    changes.apply(project, taskDir, targetTask)
    if (taskFilesChanged) {
      YamlFormatSynchronizer.saveItem(targetTask)
    }
    logTiming("applyChanges")

    // 7. Recreate non-propagatable files (test files, hidden files) from target task definition
    // These files are stage-specific, so we need to recreate them explicitly during navigation
    recreateNonPropagatableFiles(project, taskDir, currentTask, targetTask)
    logTiming("recreateNonPropagatableFiles")

    // 8. ALT-10961: Force save all documents and refresh VFS to ensure changes are visible in editor
    // Document changes may be in memory but not persisted or reflected in the editor
    invokeAndWaitIfNeeded {
      FileDocumentManager.getInstance().saveAllDocuments()
      VfsUtil.markDirtyAndRefresh(false, true, true, taskDir)
      LOG.info("Navigation: Saved documents and refreshed VFS for taskDir=${taskDir.path}")
    }
    logTiming("saveDocumentsAndRefreshVFS")

    // 9. Save snapshot for target stage after forward navigation.
    // Skip if merge commit was already created (to avoid redundant commits).
    // Only save for:
    // - Target without storage (first visit to this stage)
    // - Navigation without merge (ancestor check passed, no Keep/Replace dialog)
    if (taskIndexDelta > 0 && !mergeCommitCreated) {
      // Read ALL files from disk, including user-created files
      val finalDiskState = getAllFilesFromTaskDir(taskDir, targetTask)
      val (finalPropagatableFiles, _) = finalDiskState.split(targetTask)
      val fullSnapshot = buildFullSnapshotState(targetTask, finalPropagatableFiles)
      logTiming("buildFullSnapshotState(target)")
      val message = "Navigate to '${targetTask.name}'"
      try {
        storage.saveSnapshot(targetRef, fullSnapshot, getParentRef(targetTask), message)
        LOG.info("Saved full snapshot for target task '${targetTask.name}' (ref=$targetRef): ${fullSnapshot.size} files")
      }
      catch (e: IOException) {
        LOG.error("Failed to save snapshot for target task '${targetTask.name}'", e)
      }
      logTiming("saveSnapshot(target)")
    }

    // Update HEAD to point to the current (target) task
    storage.head = targetRef
    LOG.info("HEAD updated to ref $targetRef (task '${targetTask.name}')")
    project.messageBus.syncPublisher(FrameworkStorageListener.TOPIC).headUpdated(targetRef)

    val totalTime = System.currentTimeMillis() - navigationStartTime
    LOG.info("Navigation timing: TOTAL ${totalTime}ms for '${currentTask.name}' -> '${targetTask.name}'")
  }

  /**
   * Loads non-propagatable files (test files + hidden files) from API for a task and caches them.
   * This is used as a fallback when the cache is empty (e.g., after IDE restart).
   *
   * IMPORTANT: This method must NOT block EDT. If called from EDT, it starts async loading
   * and returns null immediately. The caller should use task.taskFiles as fallback.
   *
   * @param task the task whose non-propagatable files should be loaded
   * @return map of non-propagatable files (filename -> TaskFile), or null if loading failed or is in progress
   */
  private fun loadNonPropagatableFilesFromApi(task: Task): Map<String, TaskFile>? {
    val stepId = task.id
    if (stepId <= 0) {
      LOG.warn("Cannot load non-propagatable files from API: task '${task.name}' has invalid step ID: $stepId")
      return null
    }

    // Don't block EDT - start async loading and return null
    // The caller will use task.taskFiles as fallback
    if (ApplicationManager.getApplication().isDispatchThread) {
      LOG.info("On EDT, starting async load for non-propagatable files of task '${task.name}' (step $stepId)")
      ApplicationManager.getApplication().executeOnPooledThread {
        loadNonPropagatableFilesFromApiSync(task)
      }
      return null
    }

    return loadNonPropagatableFilesFromApiSync(task)
  }

  /**
   * Synchronously loads non-propagatable files from API. Must NOT be called from EDT.
   */
  private fun loadNonPropagatableFilesFromApiSync(task: Task): Map<String, TaskFile>? {
    val stepId = task.id
    LOG.info("Loading non-propagatable files from API for task '${task.name}' (step $stepId)")

    return try {
      // ALT-10961: Use anonymous request to get original files from API.
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

      // Filter non-propagatable files: author-created AND (invisible OR non-editable)
      // This includes test files and hidden configuration files
      val nonPropagatableFiles = allFiles.filter { taskFile ->
        !taskFile.isLearnerCreated && (!taskFile.isVisible || !taskFile.isEditable)
      }

      if (nonPropagatableFiles.isEmpty()) {
        LOG.info("Step $stepId has no non-propagatable files")
        // Store empty map so we don't keep trying to load
        originalNonPropagatableFilesCache[stepId] = emptyMap()
        return emptyMap()
      }

      // Create copies of TaskFile objects and cache them
      val copiedFiles = nonPropagatableFiles.associateBy(
        { it.name },
        { taskFile ->
          TaskFile(taskFile.name, taskFile.contents).also {
            it.isVisible = taskFile.isVisible
            it.isEditable = taskFile.isEditable
            it.isLearnerCreated = taskFile.isLearnerCreated
          }
        }
      )

      originalNonPropagatableFilesCache[stepId] = copiedFiles
      val filesInfo = copiedFiles.entries.joinToString { (name, file) ->
        "$name:size=${file.contents.textualRepresentation.length}"
      }
      LOG.info("Loaded and cached ${copiedFiles.size} non-propagatable files from API for task '${task.name}' (step $stepId): [$filesInfo]")

      copiedFiles
    }
    catch (e: Exception) {
      LOG.warn("Exception while loading non-propagatable files from API for step $stepId", e)
      null
    }
  }

  /**
   * Recreates non-propagatable files (test files, hidden files) from storage snapshot, cache, or API.
   * Non-propagatable files are provided by the course author and should not be modified by students.
   *
   * Priority for getting files:
   * 1. In-memory cache - most reliable for current session
   * 2. API request - fresh data from server
   * 3. task.taskFiles - last resort fallback
   *
   * @param currentTask The task we're navigating FROM (used to identify old files to delete)
   * @param targetTask The task we're navigating TO (used to identify new files to create)
   */
  private fun recreateNonPropagatableFiles(project: Project, taskDir: VirtualFile, currentTask: Task, targetTask: Task) {
    // Get non-propagatable files for current task (to know what to delete)
    val currentNonPropagatableFileNames = getNonPropagatableFileNames(currentTask)

    // Get non-propagatable files for target task from cache or API
    val targetNonPropagatableFiles = getNonPropagatableFilesWithMetadata(targetTask)
    val targetNonPropagatableFileNames = targetNonPropagatableFiles.keys

    // Delete files from current task that are not in target task
    val targetPropagatableFileNames = targetTask.taskFiles
      .filterValues { taskFile -> taskFile.shouldBePropagated() }
      .keys
    val filesToDelete = currentNonPropagatableFileNames - targetNonPropagatableFileNames - targetPropagatableFileNames
    if (filesToDelete.isNotEmpty()) {
      LOG.info("Deleting ${filesToDelete.size} old non-propagatable files: $filesToDelete")
      invokeAndWaitIfNeeded {
        runWriteAction {
          // Collect parent directories that may become empty after file deletion
          val potentiallyEmptyDirs = mutableSetOf<VirtualFile>()
          for (fileName in filesToDelete) {
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
              LOG.warn("Failed to delete old non-propagatable file $fileName", e)
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

    // Create non-propagatable files for target task
    if (targetNonPropagatableFiles.isNotEmpty()) {
      val filesInfo = targetNonPropagatableFiles.entries.joinToString { "${it.key}:${it.value.contents.textualRepresentation.hashCode()}" }
      LOG.info("Recreating ${targetNonPropagatableFiles.size} non-propagatable files for task '${targetTask.name}' (step ${targetTask.id}): [$filesInfo]")

      for ((filePath, taskFile) in targetNonPropagatableFiles) {
        try {
          // createChildFile handles write action internally via runInWriteActionAndWait
          // Use the file's isEditable property (test files are non-editable, some hidden files may be editable)
          val createdFile = GeneratorUtils.createChildFile(
            project,
            taskDir,
            filePath,
            taskFile.contents.textualRepresentation,
            taskFile.isEditable
          )
          if (createdFile == null) {
            LOG.error("Failed to create non-propagatable file $filePath - createChildFile returned null")
          }
          else {
            LOG.info("Successfully created non-propagatable file: ${createdFile.path}")
          }
        }
        catch (e: Exception) {
          LOG.error("Exception while recreating non-propagatable file $filePath for task ${targetTask.name}", e)
        }
      }
    }
  }

  /**
   * Gets non-propagatable file names for a task (for deletion during navigation).
   */
  private fun getNonPropagatableFileNames(task: Task): Set<String> {
    // Try cache first
    val cached = originalNonPropagatableFilesCache[task.id]
    if (cached != null) {
      return cached.keys
    }

    // Fallback to task model
    return task.taskFiles.filterValues { taskFile ->
      !taskFile.isLearnerCreated && !taskFile.shouldBePropagated()
    }.keys
  }

  /**
   * Gets non-propagatable files with metadata (TaskFile objects) for a task.
   * Used when creating files to preserve isEditable property.
   * Returns map of path -> TaskFile.
   */
  private fun getNonPropagatableFilesWithMetadata(task: Task): Map<String, TaskFile> {
    // 1. Try in-memory cache
    val cached = originalNonPropagatableFilesCache[task.id]
    if (cached != null) {
      LOG.info("Got ${cached.size} non-propagatable files from cache for task '${task.name}'")
      return cached
    }

    // 2. Try loading from API
    LOG.info("No cached non-propagatable files for task '${task.name}', loading from API...")
    if (ApplicationManager.getApplication().isDispatchThread) {
      try {
        ApplicationManager.getApplication().executeOnPooledThread<Unit> {
          loadNonPropagatableFilesFromApiSync(task)
        }.get()
        val loaded = originalNonPropagatableFilesCache[task.id]
        if (loaded != null) {
          return loaded
        }
      } catch (e: Exception) {
        LOG.warn("Failed to load non-propagatable files from API for task '${task.name}'", e)
      }
    } else {
      loadNonPropagatableFilesFromApi(task)
      val loaded = originalNonPropagatableFilesCache[task.id]
      if (loaded != null) {
        return loaded
      }
    }

    // 3. Fallback to task.taskFiles
    LOG.warn("All sources failed, falling back to task.taskFiles for task '${task.name}'")
    return task.taskFiles.filterValues { taskFile ->
      !taskFile.isLearnerCreated && !taskFile.shouldBePropagated()
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
        // Use buildFullSnapshotState to include both user files and test files from target task
        val fullSnapshot = buildFullSnapshotState(targetTask, targetPropagatableFilesState)
        storage.saveMergeSnapshot(targetRef, fullSnapshot, listOf(targetRef, currentRef), mergeMessage)
        LOG.info("Created auto-Keep merge commit for '$targetRef' with parents [$targetRef, $currentRef]: ${fullSnapshot.size} files")
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
    // We check if file is invisible AND not in a test directory (test files are handled by recreateNonPropagatableFiles)
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

    val currentHasLearnerChanges = hasPropagatableChangesFromOriginalTemplate(currentTask, currentPropagatableFilesState)
    val targetHasLearnerChanges = hasPropagatableChangesFromOriginalTemplate(targetTask, targetPropagatableFilesState)
    LOG.info(
      "Merge user-change check: currentRef=$currentRef, targetRef=$targetRef, " +
      "currentHasLearnerChanges=$currentHasLearnerChanges, targetHasLearnerChanges=$targetHasLearnerChanges"
    )

    when {
      currentHasLearnerChanges && !targetHasLearnerChanges -> {
        LOG.info("Auto-Replace for '$targetRef': only current task has learner changes")
        saveMergeSnapshot(
          targetTask,
          targetRef,
          currentRef,
          currentPropagatableFilesState,
          "Merge from '${currentTask.name}': Auto-replace learner changes"
        )
        return calculateCurrentTaskChanges()
      }
      !currentHasLearnerChanges && targetHasLearnerChanges -> {
        LOG.info("Auto-Keep for '$targetRef': only target task has learner changes")
        saveMergeSnapshot(
          targetTask,
          targetRef,
          currentRef,
          targetPropagatableFilesState,
          "Merge from '${currentTask.name}': Auto-keep target learner changes"
        )
        return calculateChanges(currentState, targetState)
      }
      !currentHasLearnerChanges && !targetHasLearnerChanges -> {
        LOG.info("Auto-Replace for '$targetRef': neither task has learner changes")
        saveMergeSnapshot(
          targetTask,
          targetRef,
          currentRef,
          currentPropagatableFilesState,
          "Merge from '${currentTask.name}': Auto-replace author updates"
        )
        return calculateCurrentTaskChanges()
      }
    }

    // If target snapshot has no propagatable files (empty or only test/hidden files) - auto-Replace without dialog
    // Such commits don't represent user changes, so there's nothing meaningful to keep - propagate current state
    if (newTargetPropagatableFilesState.isEmpty()) {
      LOG.info("Target snapshot has no propagatable files, auto-Replace: propagating current state without dialog")
      val mergeMessage = "Merge from '${currentTask.name}': Auto-replace (target had no user files)"
      try {
        val currentFileEntries = currentPropagatableFilesState.toFileEntries(currentTask, originalNonPropagatableFilesCache[currentTask.id])
        storage.saveMergeSnapshot(targetRef, currentFileEntries, listOf(targetRef, currentRef), mergeMessage)
        LOG.info("Created auto-Replace merge commit for '$targetRef' with parents [$targetRef, $currentRef]")
      } catch (e: IOException) {
        LOG.error("Failed to create auto-Replace merge commit for '$targetRef'", e)
      }
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

    // Target has saved state with propagatable files AND content differs - ask user before overwriting
    val keepChanges = if (showDialogIfConflict) {
      val currentTaskName = "${currentTask.getUIName()} ${currentTask.index}"
      val targetTaskName = "${targetTask.getUIName()} ${targetTask.index}"
      val result = PropagationConflictDialog.show(
        project,
        currentTaskName,
        targetTaskName,
        newCurrentPropagatableFilesState,
        newTargetPropagatableFilesState
      )
      result == PropagationConflictDialog.Result.KEEP
    }
    else {
      // When dialog is suppressed, default to Keep (preserve target's content).
      // This is the safest default for automated scenarios (e.g., lesson updates).
      // For project open, the caller should pass showDialogIfConflict=true to let user decide.
      LOG.info("Dialog suppressed, using Keep to preserve target content for '${targetTask.name}'")
      true
    }

    return if (keepChanges) {
      // User chose Keep - create merge commit with target's content
      // This records that we "merged" but kept our version (like git merge with "ours" strategy)
      propagationActive = false
      val mergeMessage = "Merge from '${currentTask.name}': Keep target changes"
      try {
        // Use buildFullSnapshotState to include both user files and test files from target task
        val fullSnapshot = buildFullSnapshotState(targetTask, targetPropagatableFilesState)
        storage.saveMergeSnapshot(targetRef, fullSnapshot, listOf(targetRef, currentRef), mergeMessage)
        LOG.info("Created Keep merge commit for '$targetRef' with parents [$targetRef, $currentRef]: ${fullSnapshot.size} files")
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
        // Use buildFullSnapshotState with targetTask to get target's test files,
        // but with currentPropagatableFilesState to propagate user's code changes
        val fullSnapshot = buildFullSnapshotState(targetTask, currentPropagatableFilesState)
        storage.saveMergeSnapshot(targetRef, fullSnapshot, listOf(targetRef, currentRef), mergeMessage)
        LOG.info("Created Replace merge commit for '$targetRef' with parents [$targetRef, $currentRef]: ${fullSnapshot.size} files")
      } catch (e: IOException) {
        LOG.error("Failed to create Replace merge commit for '$targetRef'", e)
      }
      calculateCurrentTaskChanges()
    }
  }

  /**
   * Calculates changes for first visit to a new stage (forward navigation without existing snapshot).
   *
   * The key difference from [calculateChanges]:
   * - For propagatable files: keep user's current state, only ADD new files from target templates
   * - For non-propagatable files: use target templates (user couldn't modify them anyway)
   *
   * This preserves user's code while adding any new template files the author added for this stage.
   *
   * @param currentState Current propagatable files from disk (user's code)
   * @param targetState Target templates (all visible non-test files from target task)
   * @param targetTask The task we're navigating to (used to determine file propagation status)
   */
  private fun calculateFirstVisitChanges(
    currentState: FLTaskState,
    targetState: FLTaskState,
    currentTask: Task,
    targetTask: Task
  ): UserChanges {
    val changes = mutableListOf<Change>()

    // 1. Propagate user-created files from current state that are NOT in target template
    for ((path, text) in currentState) {
      if (path !in targetState) {
        LOG.info("First visit: propagating user-created file '$path'")
        changes += Change.PropagateLearnerCreatedTaskFile(path, text)
      }
    }

    // 2. Handle files from target template
    for ((path, text) in targetState) {
      val taskFile = targetTask.taskFiles[path]
      val isPropagatable = taskFile?.shouldBePropagated() ?: true

      if (isPropagatable) {
        if (path !in currentState) {
          val currentTaskFile = currentTask.taskFiles[path]
          val originalCurrentTemplate = originalTemplateFilesCache[currentTask.id]
          val wasInCurrentTemplate = originalCurrentTemplate?.containsKey(path) ?: false
          // The learner diverged from the current task's template if they added or removed
          // propagatable files (compared with the captured original template). When diverged, their
          // current state defines the propagatable files (see FrameworkLesson.propagateFilesOnNavigation),
          // so an author template file absent from it is discarded rather than re-added.
          val learnerDiverged = originalCurrentTemplate != null &&
            currentState.keys != originalCurrentTemplate.keys
          when {
            currentTaskFile != null && !currentTaskFile.shouldBePropagated() -> {
              // The file was non-propagatable (e.g. invisible) in the current task and becomes
              // propagatable (visible) in the target task. Changes for such files are not propagated,
              // so we add the author's version.
              LOG.info("First visit: adding '$path' that became propagatable in target task")
              changes += Change.AddFile(path, text)
            }
            wasInCurrentTemplate -> {
              // The file existed in the current task's original propagatable template but is absent
              // from the learner's current state, i.e. the learner deleted it. Respect that deletion.
              LOG.info("First visit: discarding propagatable file '$path' the learner deleted")
              changes += Change.RemoveTaskFile(path)
            }
            learnerDiverged -> {
              // The learner replaced the propagatable files (added/removed files), so an author
              // template file absent from their state is discarded rather than re-added.
              LOG.info("First visit: discarding author propagatable file '$path' (learner replaced propagatable files)")
              changes += Change.RemoveTaskFile(path)
            }
            else -> {
              // Regular course: the file is a genuinely new template the author introduced in the
              // target stage (it was never part of the current task's template, so it could not have
              // been deleted by the learner). Add the author's version.
              LOG.info("First visit: adding new author template file '$path' introduced in target task")
              changes += Change.AddFile(path, text)
            }
          }
        }
        // If it's in both, we keep the user's version from currentState (it's already on disk)
      }
      else {
        // Non-propagatable files (e.g., read-only reference files):
        // Always use target version since user couldn't modify them
        LOG.info("First visit: adding non-propagatable file '$path'")
        changes += Change.AddFile(path, text)
      }
    }

    LOG.info("First visit changes: ${changes.size} changes")
    return UserChanges(changes)
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
    // Data loaded from API via loadNonPropagatableFilesFromApi() is always correct.
    if (originalNonPropagatableFilesCache.containsKey(task.id)) {
      LOG.info("Cache already contains non-propagatable files for task '${task.name}' (step ${task.id}), not overwriting")
      return
    }
    storeNonPropagatableFilesInternal(task)
  }

  override fun updateOriginalTestFiles(task: Task) {
    // Force update the cache, used when task files are updated from remote server
    // (e.g., during course update). Unlike storeOriginalTestFiles, this WILL overwrite.
    // Pass forceUpdate=true to handle the case when author removes all non-propagatable files.
    LOG.info("Force updating non-propagatable files cache for task '${task.name}' (step ${task.id})")
    storeNonPropagatableFilesInternal(task, forceUpdate = true)
  }

  override fun updateSnapshotTestFiles(task: Task) {
    require(task.lesson is FrameworkLesson) {
      "Only framework task snapshots can be updated"
    }

    val ref = task.storageRef()

    // Only update if snapshot exists (user visited this task before)
    if (!storage.hasRef(ref)) {
      LOG.info("updateSnapshotTestFiles: No snapshot exists for task '${task.name}' (ref=$ref), skipping")
      return
    }

    // Get cached non-propagatable files (should be updated via updateOriginalTestFiles before calling this)
    // Note: null means cache wasn't updated (can't proceed), empty map means author removed all non-propagatable files
    val cachedNonPropagatableFiles = originalNonPropagatableFilesCache[task.id]
    if (cachedNonPropagatableFiles == null) {
      LOG.warn("updateSnapshotTestFiles: No cached non-propagatable files for task '${task.name}', cannot update snapshot")
      return
    }

    try {
      // Get current snapshot (Map<String, FileEntry>)
      val currentSnapshot = storage.getSnapshot(ref)

      // Get propagatable file names from task model to identify user files
      val propagatableFileNames = task.taskFiles.filterValues { it.shouldBePropagated() }.keys

      // Keep user files from existing snapshot. A learner-modified file can disappear from
      // task.taskFiles after a course update, but must stay in the snapshot for navigation.
      val userFiles = currentSnapshot.filter { (path, entry) ->
        path in propagatableFileNames || entry.isPropagatable && entry.hasLearnerModification
      }

      // Combine user files with new non-propagatable files from cache
      // If cache is empty, this effectively removes all non-propagatable files from snapshot
      val updatedSnapshot = HashMap(userFiles)
      for ((path, taskFile) in cachedNonPropagatableFiles) {
        updatedSnapshot[path] = FileEntry.create(
          content = taskFile.contents.textualRepresentation,
          visible = taskFile.isVisible,
          editable = taskFile.isEditable,
          propagatable = taskFile.shouldBePropagated()
        )
      }

      // Save updated snapshot
      val message = if (cachedNonPropagatableFiles.isEmpty()) {
        "Remove all non-propagatable files from server for '${task.name}'"
      } else {
        "Update non-propagatable files from server for '${task.name}'"
      }
      val created = storage.saveSnapshot(ref, updatedSnapshot, getParentRef(task), message)
      if (created) {
        LOG.info("updateSnapshotTestFiles: Updated snapshot for task '${task.name}' with ${cachedNonPropagatableFiles.size} non-propagatable files")
      } else {
        LOG.info("updateSnapshotTestFiles: Snapshot unchanged for task '${task.name}' (files identical)")
      }
    }
    catch (e: IOException) {
      LOG.error("Failed to update snapshot non-propagatable files for task '${task.name}'", e)
    }
  }

  private fun storeNonPropagatableFilesInternal(task: Task, forceUpdate: Boolean = false) {
    // Non-propagatable files: test files + hidden files (invisible or non-editable)
    val nonPropagatableFiles = task.taskFiles.filterValues { taskFile ->
      !taskFile.isLearnerCreated && !taskFile.shouldBePropagated()
    }
    if (nonPropagatableFiles.isNotEmpty()) {
      // Create copies of TaskFile objects to prevent modification when original task.taskFiles changes
      // This is important because task.taskFiles contents can be updated (e.g., during Update Course)
      // and we want to preserve the original file contents
      val copiedFiles = nonPropagatableFiles.mapValues { (_, taskFile) ->
        TaskFile(taskFile.name, taskFile.contents).also {
          it.isVisible = taskFile.isVisible
          it.isEditable = taskFile.isEditable
          it.isLearnerCreated = taskFile.isLearnerCreated
        }
      }
      originalNonPropagatableFilesCache[task.id] = copiedFiles
      val filesInfo = copiedFiles.entries.joinToString { (name, file) ->
        "$name:size=${file.contents.textualRepresentation.length}"
      }
      LOG.info("Stored ${copiedFiles.size} original non-propagatable files for task '${task.name}' (step ${task.id}): [$filesInfo]")
    } else if (forceUpdate) {
      // Author removed all non-propagatable files - store empty map to reflect this
      originalNonPropagatableFilesCache[task.id] = emptyMap()
      LOG.info("Stored empty non-propagatable files cache for task '${task.name}' (step ${task.id})")
    }
  }

  override fun getOriginalTestFiles(task: Task): Collection<TaskFile>? {
    return originalNonPropagatableFilesCache[task.id]?.values
  }

  override fun ensureTestFilesCached(task: Task): Boolean {
    if (originalNonPropagatableFilesCache.containsKey(task.id)) {
      return true
    }
    // Cache is empty, try to load from API
    return loadNonPropagatableFilesFromApi(task) != null
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

    LOG.debug("storeOriginalTemplateFiles: task='${task.name}', taskFiles=${task.taskFiles.keys}")
    task.taskFiles.forEach { (name, taskFile) ->
      LOG.debug("storeOriginalTemplateFiles: file='$name', isVisible=${taskFile.isVisible}, isTestFile=${taskFile.isTestFile}")
    }

    val templateFiles = task.taskFiles.filterValues { taskFile ->
      taskFile.isVisible && !taskFile.isTestFile && !taskFile.isLearnerCreated
    }
    LOG.debug("storeOriginalTemplateFiles: filtered templateFiles=${templateFiles.keys}")

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

  override fun updateOriginalTemplateFiles(task: Task) {
    // Force update the cache, used when task files are updated from remote server
    // (e.g., during course update). Unlike storeOriginalTemplateFiles, this WILL overwrite.
    LOG.info("Force updating template files cache for task '${task.name}' (step ${task.id})")
    updateOriginalTemplateFilesCache(task, task.taskFiles)
  }

  private fun updateOriginalTemplateFilesCache(task: Task, taskFiles: Map<String, TaskFile>) {
    val templateFiles = taskFiles.filterValues { taskFile ->
      taskFile.isVisible && !taskFile.isTestFile && !taskFile.isLearnerCreated
    }

    val cachedTemplates = templateFiles.mapValues { (_, taskFile) ->
      taskFile.contents.textualRepresentation
    }
    originalTemplateFilesCache[task.id] = cachedTemplates
    val filesInfo = cachedTemplates.entries.joinToString { (name, content) ->
      "$name:size=${content.length}"
    }
    LOG.info("Updated ${cachedTemplates.size} original template files for task '${task.name}' (step ${task.id}): [$filesInfo]")
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
    originalNonPropagatableFilesCache.clear()
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

    // Read current disk state (all files including user-created).
    // Note: do NOT early-return when propagatableFiles is empty — the user may have legitimately
    // deleted every editable file, and the snapshot must be updated to reflect that. The equality
    // check below will short-circuit cases where there is genuinely nothing to save.
    val currentDiskState = getAllFilesFromTaskDir(taskDir, currentTask)
    val (propagatableFiles, _) = currentDiskState.split(currentTask)

    // Check if there are actual changes compared to saved snapshot (compare only user files)
    val ref = currentTask.storageRef()
    val existingSnapshot = try {
      if (storage.hasRef(ref)) storage.getSnapshot(ref).toContentMap() else emptyMap()
    } catch (e: IOException) {
      emptyMap()
    }

    // Extract only user files from existing snapshot for comparison
    val existingUserFiles = existingSnapshot.filterKeys { path ->
      currentTask.getTaskFile(path)?.shouldBePropagated() ?: true
    }
    if (propagatableFiles == existingUserFiles) {
      // No changes to save
      return
    }

    // Build full snapshot: user files from disk + non-propagatable files from cache
    val fullSnapshot = buildFullSnapshotState(currentTask, propagatableFiles)

    // Save full snapshot
    try {
      val created = storage.saveSnapshot(ref, fullSnapshot, getParentRef(currentTask), "Auto-save changes for '${currentTask.name}'")
      if (created) {
        LOG.info("Auto-saved full snapshot for task '${currentTask.name}' (ref=$ref): ${fullSnapshot.size} files")
      }
    } catch (e: IOException) {
      LOG.warn("Failed to auto-save snapshot for task '${currentTask.name}'", e)
    }
  }

  /**
   * Returns the state of all non-test files for this task (user-editable files).
   * This includes both template files (visible, editable) and learner-created files.
   *
   * Test files are excluded here because they are handled separately.
   * For full snapshot including tests, use [getFullTaskState].
   */
  private val Task.allFiles: FLTaskState
    get() = taskFiles
      .filterValues { !it.isTestFile }
      .mapValues { it.value.contents.textualRepresentation }

  /**
   * Returns ALL files for this task including test files.
   * Used for creating complete snapshots. Learner-created files are excluded because
   * this task model state represents author-provided files; learner files are read from disk.
   */
  private val Task.allFilesIncludingTests: FLTaskState
    get() = taskFiles
      .filterValues { !it.isLearnerCreated }
      .mapValues { it.value.contents.textualRepresentation }

  /**
   * Reads ALL files from task directory, including user-created files.
   * This is needed to capture user-created files that are not in the template.
   *
   * @param taskDir The task directory to read files from
   * @param task The task (used to filter out test directories)
   * @return Map of file paths to content
   */
  private fun getAllFilesFromTaskDir(taskDir: VirtualFile, task: Task): FLTaskState {
    val result = HashMap<String, String>()
    val documentManager = FileDocumentManager.getInstance()
    val testDirs = task.testDirs
    val configurator = task.course.configurator

    // Recursively collect all files from task directory
    fun collectFiles(dir: VirtualFile, pathPrefix: String = "") {
      for (child in dir.children) {
        if (configurator?.excludeFromArchive(project, child) == true) continue
        val relativePath = if (pathPrefix.isEmpty()) child.name else "$pathPrefix/${child.name}"

        if (child.isDirectory) {
          // Skip test directories - they will be handled separately
          val isTestDir = testDirs.any { testDir ->
            relativePath == testDir || relativePath.startsWith("$testDir/")
          }
          if (!isTestDir) {
            collectFiles(child, relativePath)
          }
        }
        else {
          // Read file content
          val text = if (child.isToEncodeContent) {
            child.loadEncodedContent(isToEncodeContent = true)
          }
          else {
            runReadAction { documentManager.getDocument(child)?.text }
          }

          if (text != null) {
            result[relativePath] = text
          }
        }
      }
    }

    collectFiles(taskDir)
    return result.withoutDeletedTemplateFiles(task)
  }

  private fun FLTaskState.withoutDeletedTemplateFiles(task: Task): FLTaskState {
    val originalTemplateFiles = originalTemplateFilesCache[task.id]
    val deletedTemplateFiles = keys - task.taskFiles.keys
    if (deletedTemplateFiles.isEmpty()) return this

    val storedSnapshot = try {
      if (storage.hasRef(task.storageRef())) storage.getSnapshot(task.storageRef()) else emptyMap()
    }
    catch (e: IOException) {
      LOG.warn("Failed to load snapshot while filtering deleted template files for '${task.name}'", e)
      return this
    }

    val filtered = filter { (path, text) ->
      if (path !in deletedTemplateFiles) return@filter true
      if (storedSnapshot[path]?.hasLearnerModification == true) return@filter true

      val originalText = originalTemplateFiles?.get(path)
      when {
        originalText != null -> text != originalText
        storedSnapshot.containsKey(path) -> storedSnapshot[path]?.content != text
        else -> true
      }
    }
    if (filtered.size != size) {
      LOG.info(
        "Filtered deleted template files from disk state for '${task.name}': " +
        (keys - filtered.keys).joinToString()
      )
    }
    return filtered
  }

  /**
   * Builds complete task state for snapshot: user files from disk + non-propagatable files from cache.
   * Non-propagatable files (test files, hidden files) are taken from cache (not disk)
   * because disk may have files from another stage.
   *
   * Returns FileEntry objects with metadata (visible, editable, propagatable) extracted from TaskFile objects.
   *
   * @param task The task to build state for
   * @param userFilesFromDisk User files read from disk (propagatable files)
   * @return Complete state with user files and non-propagatable files as FileEntry objects
   */
  private fun buildFullSnapshotState(
    task: Task,
    userFilesFromDisk: FLTaskState
  ): Map<String, FileEntry> {
    val result = HashMap<String, FileEntry>()
    val testDirs = task.testDirs

    // Add user files with metadata from task.taskFiles or path patterns
    for ((path, content) in userFilesFromDisk) {
      result[path] = resolveFileEntryMetadata(path, content, task, testDirs)
    }

    // Add non-propagatable files from cache, API, or task.taskFiles (in that priority order)
    val nonPropagatableFiles = getNonPropagatableFilesWithMetadata(task)
    for ((path, taskFile) in nonPropagatableFiles) {
      val content = taskFile.contents.textualRepresentation
      result[path] = resolveFileEntryMetadata(path, content, task, testDirs)
    }

    return result
  }

  /**
   * Resolves metadata for a file entry using the following priority:
   * 1. Path patterns (test files are ALWAYS non-visible, non-propagatable)
   * 2. task.taskFiles (from API)
   * 3. Default metadata (visible, editable, propagatable)
   */
  private fun resolveFileEntryMetadata(
    path: String,
    content: String,
    task: Task,
    testDirs: List<String>,
    taskFiles: Map<String, TaskFile> = task.taskFiles
  ): FileEntry {
    // 1. Path patterns have highest priority - test files are ALWAYS hidden/non-propagatable
    if (FileEntry.isTestFilePath(path, testDirs)) {
      return FileEntry.create(content, visible = false, editable = false, propagatable = false)
    }

    // 2. Check task.taskFiles - metadata from API (author's intent)
    val taskFile = taskFiles[path]
    if (taskFile != null) {
      return FileEntry.create(
        content = content,
        visible = taskFile.isVisible,
        editable = taskFile.isEditable,
        propagatable = taskFile.shouldBePropagated()
      )
    }

    // 3. Default: user file (visible, editable, propagatable)
    return FileEntry(content)
  }

  private fun FLTaskState.splitByKey(predicate: (String) -> Boolean): Pair<FLTaskState, FLTaskState> {
    val positive = HashMap<String, String>()
    val negative = HashMap<String, String>()

    for ((path, text) in this) {
      val state = if (predicate(path)) positive else negative
      state[path] = text
    }

    return positive to negative
  }

  private fun saveMergeSnapshot(
    targetTask: Task,
    targetRef: String,
    currentRef: String,
    propagatableFilesState: FLTaskState,
    message: String
  ) {
    try {
      val fullSnapshot = buildFullSnapshotState(targetTask, propagatableFilesState)
      storage.saveMergeSnapshot(targetRef, fullSnapshot, listOf(targetRef, currentRef), message)
      LOG.info("Created merge commit for '$targetRef' with parents [$targetRef, $currentRef]: ${fullSnapshot.size} files")
    }
    catch (e: IOException) {
      LOG.error("Failed to create merge commit for '$targetRef'", e)
    }
  }

  private fun FileEntry.withLearnerModification(): FileEntry {
    return copy(metadata = metadata + (LEARNER_MODIFIED_METADATA_KEY to true))
  }

  private val FileEntry.hasLearnerModification: Boolean
    get() = metadata[LEARNER_MODIFIED_METADATA_KEY] as? Boolean == true

  private fun hasPropagatableChangesFromOriginalTemplate(task: Task, propagatableFilesState: FLTaskState): Boolean {
    try {
      if (storage.hasRef(task.storageRef())) {
        val learnerModifiedFiles = storage.getSnapshot(task.storageRef())
          .filter { (path, entry) -> path in propagatableFilesState && entry.hasLearnerModification }
        if (learnerModifiedFiles.isNotEmpty()) {
          LOG.info(
            "hasPropagatableChangesFromOriginalTemplate: task='${task.name}' has learner-modified files: " +
            learnerModifiedFiles.keys.joinToString()
          )
          return true
        }
      }
    }
    catch (e: IOException) {
      LOG.warn("Failed to inspect learner modification metadata for '${task.name}'", e)
    }

    val originalTemplateFiles = originalTemplateFilesCache[task.id]
    if (originalTemplateFiles == null) {
      LOG.info("hasPropagatableChangesFromOriginalTemplate: no template cache for '${task.name}', falling back to parent comparison")
      return hasPropagatableChangesFromParent(task.storageRef(), task)
    }

    val (originalPropagatableFiles, _) = originalTemplateFiles.split(task)
    val hasChanges = propagatableFilesState != originalPropagatableFiles
    LOG.info(
      "hasPropagatableChangesFromOriginalTemplate: task='${task.name}', hasChanges=$hasChanges " +
      "(current=${propagatableFilesState.size} files, original=${originalPropagatableFiles.size} files)"
    )
    return hasChanges
  }

  /**
   * Checks if the current ref has propagatable file changes compared to its parent commit.
   * Returns true if:
   * - There's no parent (first commit) - considered as having changes
   * - Propagatable files differ from parent - user made code changes
   * Returns false if:
   * - Only non-propagatable files (tests, hidden) changed - no user code changes
   *
   * This is used to skip Keep/Replace dialog when only test files were updated
   * (e.g., author updated tests via Update Course).
   */
  private fun hasPropagatableChangesFromParent(ref: String, task: Task): Boolean {
    val commitHash = storage.resolveRef(ref)
    if (commitHash == null) {
      LOG.info("hasPropagatableChangesFromParent: ref=$ref has no commit, assuming has changes")
      return true
    }

    val commit = storage.getCommit(commitHash)
    if (commit == null) {
      LOG.info("hasPropagatableChangesFromParent: commit=$commitHash not found, assuming has changes")
      return true
    }

    val parentHash = commit.parentHashes.firstOrNull()
    if (parentHash == null) {
      // First commit in the chain - considered as having changes
      LOG.info("hasPropagatableChangesFromParent: ref=$ref is first commit, assuming has changes")
      return true
    }

    val parentCommit = storage.getCommit(parentHash)
    if (parentCommit == null) {
      LOG.info("hasPropagatableChangesFromParent: parent commit=$parentHash not found, assuming has changes")
      return true
    }

    try {
      val currentSnapshot = storage.getSnapshot(ref).toContentMap()
      val parentSnapshot = storage.getSnapshotByHash(parentCommit.snapshotHash)?.toContentMap()
      if (parentSnapshot == null) {
        LOG.info("hasPropagatableChangesFromParent: parent snapshot not found, assuming has changes")
        return true
      }

      // Compare only propagatable files
      val (currentPropagatable, _) = currentSnapshot.split(task)
      val (parentPropagatable, _) = parentSnapshot.split(task)

      val hasChanges = currentPropagatable != parentPropagatable
      LOG.info("hasPropagatableChangesFromParent: ref=$ref, hasChanges=$hasChanges (current=${currentPropagatable.size} files, parent=${parentPropagatable.size} files)")
      return hasChanges
    } catch (e: Exception) {
      LOG.warn("hasPropagatableChangesFromParent: error comparing snapshots, assuming has changes", e)
      return true
    }
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
      LOG.debug("split: path='$path' excluded, isVisible=${taskFile.isVisible}, isEditable=${taskFile.isEditable}")
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
    originalNonPropagatableFilesCache.clear()
    originalTemplateFilesCache.clear()
  }

  override fun syncCurrentTaskIndexFromStorage(lesson: FrameworkLesson): Boolean {
    val head = storage.head

    if (head != null) {
      // HEAD exists - find the task whose storageRef matches HEAD
      val taskIndex = lesson.taskList.indexOfFirst { it.storageRef() == head }
      if (taskIndex == -1) {
        LOG.debug("syncCurrentTaskIndexFromStorage: HEAD=$head but no task found with matching storageRef")
        // Still mark as synced to allow auto-save to work
        isStorageSynced = true
        return false
      }

      if (lesson.currentTaskIndex != taskIndex) {
        LOG.info("syncCurrentTaskIndexFromStorage: syncing currentTaskIndex from ${lesson.currentTaskIndex} to $taskIndex (HEAD=$head)")
        lesson.currentTaskIndex = taskIndex
      }

      isStorageSynced = true
      return true
    }

    // No HEAD means fresh storage - calculate current task from task statuses
    // (first unsolved task, or last task if all are solved)
    val firstUnsolvedIndex = lesson.taskList.indexOfFirst { it.status != CheckStatus.Solved }
    val calculatedIndex = if (firstUnsolvedIndex != -1) {
      firstUnsolvedIndex
    } else if (lesson.taskList.isNotEmpty()) {
      lesson.taskList.size - 1  // All solved, use last task
    } else {
      0  // Empty lesson, use 0
    }

    LOG.info("syncCurrentTaskIndexFromStorage: no HEAD, calculated currentTaskIndex=$calculatedIndex from task statuses")

    if (lesson.currentTaskIndex != calculatedIndex) {
      LOG.info("syncCurrentTaskIndexFromStorage: updating currentTaskIndex from ${lesson.currentTaskIndex} to $calculatedIndex")
      lesson.currentTaskIndex = calculatedIndex
    }

    // Set HEAD to the calculated task's ref so future syncs use storage
    val calculatedTask = lesson.taskList.getOrNull(calculatedIndex)
    if (calculatedTask != null) {
      val ref = calculatedTask.storageRef()
      storage.head = ref
      LOG.info("syncCurrentTaskIndexFromStorage: initialized HEAD to $ref")
    }

    isStorageSynced = true
    return false
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
      LOG.debug("CREATE_STORAGE: path=$storageFilePath, exists=$storageExists")

      try {
        val storage = FrameworkStorage(storageFilePath)
        storage.migrate(VERSION)
        LOG.debug("CREATE_STORAGE: success, version=${storage.version}")
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
