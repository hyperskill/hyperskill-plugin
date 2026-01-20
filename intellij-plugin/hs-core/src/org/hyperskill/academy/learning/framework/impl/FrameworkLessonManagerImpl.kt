package org.hyperskill.academy.learning.framework.impl

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.SlowOperations
import com.intellij.util.io.storage.AbstractStorage
import com.intellij.openapi.application.ApplicationManager
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.isTestFile
import org.hyperskill.academy.learning.courseFormat.ext.shouldBePropagated
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.hyperskill.academy.learning.framework.propagateFilesOnNavigation
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.toCourseInfoHolder
import org.hyperskill.academy.learning.ui.getUIName
import org.hyperskill.academy.learning.stepik.PyCharmStepOptions
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
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

  override fun saveExternalChanges(task: Task, externalState: Map<String, String>) {
    require(task.lesson is FrameworkLesson) {
      "Only solutions of framework tasks can be saved"
    }

    LOG.info("saveExternalChanges: task='${task.name}' (id=${task.id}), externalState=${externalState.size} files")

    val propagatableFiles = task.allFiles.split(task).first
    LOG.info("saveExternalChanges: task='${task.name}' - propagatableFiles (templates): " +
             "[${propagatableFiles.entries.joinToString { "${it.key}:${it.value.length}chars,hash=${it.value.hashCode()}" }}]")

    // there may be visible editable files (f. e. binary files) that are not stored into submissions,
    // but we don't want to lose them after applying submission into a task
    val externalPropagatableFiles = externalState.split(task).first.toMutableMap()
    LOG.info("saveExternalChanges: task='${task.name}' - externalPropagatableFiles (submission): " +
             "[${externalPropagatableFiles.entries.joinToString { "${it.key}:${it.value.length}chars,hash=${it.value.hashCode()}" }}]")

    propagatableFiles.forEach { (path, text) ->
      externalPropagatableFiles.putIfAbsent(path, text)
    }
    val changes = calculateChanges(propagatableFiles, externalPropagatableFiles)
    LOG.info("saveExternalChanges: task='${task.name}' - calculated ${changes.changes.size} changes: " +
             "[${changes.changes.joinToString { it.javaClass.simpleName }}]")

    val currentRecord = task.record
    task.record = try {
      storage.updateUserChanges(currentRecord, changes)
    }
    catch (e: IOException) {
      LOG.error("Failed to save solution for task `${task.name}`", e)
      currentRecord
    }
    LOG.info("saveExternalChanges: task='${task.name}' - record updated from $currentRecord to ${task.record}")
    YamlFormatSynchronizer.saveItem(task)
  }

  override fun updateUserChanges(task: Task, newInitialState: Map<String, String>) {
    require(task.lesson is FrameworkLesson) {
      "Only solutions of framework tasks can be saved"
    }

    val currentRecord = task.record
    if (currentRecord == -1) return

    val changes = try {
      storage.getUserChanges(currentRecord)
    }
    catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${task.name}`", e)
      return
    }

    val newChanges = changes.changes.mapNotNull {
      when (it) {
        is Change.AddFile -> if (it.path in newInitialState) Change.ChangeFile(it.path, it.text) else it
        is Change.RemoveFile -> if (it.path !in newInitialState) null else it
        is Change.ChangeFile -> if (it.path !in newInitialState) Change.AddFile(it.path, it.text) else it
        is Change.PropagateLearnerCreatedTaskFile,
        is Change.RemoveTaskFile -> it
      }
    }

    try {
      storage.updateUserChanges(currentRecord, UserChanges(newChanges))
    }
    catch (e: IOException) {
      LOG.error("Failed to update user changes for task `${task.name}`", e)
    }
  }

  override fun getChangesTimestamp(task: Task): Long {
    require(task.lesson is FrameworkLesson) {
      "Changes timestamp makes sense only for framework tasks"
    }

    return storage.getUserChanges(task.record).timestamp
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

    val currentRecord = currentTask.record
    val targetRecord = targetTask.record
    LOG.info("Records: current=$currentRecord, target=$targetRecord")

    val initialCurrentFiles = currentTask.allFiles
    LOG.info("initialCurrentFiles (from currentTask.allFiles): ${initialCurrentFiles.mapValues { it.value.length.toString() + " chars, hash=" + it.value.hashCode() }}")

    // 1. Get difference between initial state of current task and previous task state
    // and construct previous state of current task.
    // Previous state is needed to determine if a user made any new change
    val previousCurrentUserChanges = getUserChangesFromStorage(currentTask)
    val previousCurrentState = HashMap(initialCurrentFiles).apply { previousCurrentUserChanges.apply(this) }
    LOG.info("previousCurrentState (after applying storage changes): ${previousCurrentState.mapValues { it.value.length.toString() + " chars, hash=" + it.value.hashCode() }}")

    // 2. Calculate difference between initial state of current task and current state on local FS.
    // Update change list for current task in [storage] to have ability to restore state of current task in future
    val (newCurrentRecord, currentUserChanges) = try {
      updateUserChanges(currentRecord, getUserChangesFromFiles(initialCurrentFiles, taskDir))
    }
    catch (e: IOException) {
      LOG.error("Failed to save user changes for task `${currentTask.name}`", e)
      UpdatedUserChanges(currentRecord, UserChanges.empty())
    }
    LOG.info("Saved currentTask changes: newRecord=$newCurrentRecord, changes=${currentUserChanges.changes.size}")

    // 3. Update record index to a new one.
    currentTask.record = newCurrentRecord
    SlowOperations.knownIssue("EDU-XXXX").use {
      YamlFormatSynchronizer.saveItem(currentTask)
    }

    // 4. Get difference (change list) between initial and latest states of target task
    val nextUserChanges = getUserChangesFromStorage(targetTask)
    LOG.info("nextUserChanges (from storage for targetTask): ${nextUserChanges.changes.size} changes")

    // 5. Apply change lists to initial state to get latest states of current and target tasks
    val currentState = HashMap(initialCurrentFiles).apply { currentUserChanges.apply(this) }
    val initialTargetFiles = targetTask.allFiles
    LOG.info("initialTargetFiles (from targetTask.allFiles): ${initialTargetFiles.mapValues { it.value.length.toString() + " chars, hash=" + it.value.hashCode() }}")
    val targetState = HashMap(initialTargetFiles).apply { nextUserChanges.apply(this) }
    LOG.info("targetState (after applying storage changes): ${targetState.mapValues { it.value.length.toString() + " chars, hash=" + it.value.hashCode() }}")

    // 6. Calculate difference between latest states of current and target tasks
    // Note, there are special rules for hyperskill courses for now
    // All user changes from the current task should be propagated to next task as is

    // If a user navigated back to current task, didn't make any change and wants to navigate to next task again
    // we shouldn't try to propagate current changes to next task
    val currentTaskHasNewUserChanges = !(currentRecord != -1 && targetRecord != -1 && previousCurrentState == currentState)
    LOG.info("currentTaskHasNewUserChanges=$currentTaskHasNewUserChanges, propagateFilesOnNavigation=${lesson.propagateFilesOnNavigation}")

    val changes = if (currentTaskHasNewUserChanges && taskIndexDelta == 1 && lesson.propagateFilesOnNavigation) {
      LOG.info("Using PROPAGATION mode")
      calculatePropagationChanges(targetTask, currentTask, currentState, targetState, showDialogIfConflict)
    }
    else {
      LOG.info("Using DIFF mode (no propagation)")
      calculateChanges(currentState, targetState)
    }
    LOG.info("Changes to apply: ${changes.changes.size} changes")

    // 7. Apply difference between latest states of current and target tasks on local FS
    changes.apply(project, taskDir, targetTask)

    // 8. Recreate test files (files with isLearnerCreated = false) from target task definition
    // These files are not tracked in framework storage, so we need to recreate them explicitly
    recreateTestFiles(project, taskDir, targetTask)

    SlowOperations.knownIssue("EDU-XXXX").use {
      YamlFormatSynchronizer.saveItem(targetTask)
    }
    LOG.info("=== Stage switch complete ===")
  }

  /**
   * Loads test files from API for a task and caches them.
   * This is used as a fallback when the cache is empty (e.g., after IDE restart).
   *
   * @param task the task whose test files should be loaded
   * @return map of test files (filename -> TaskFile), or null if loading failed
   */
  private fun loadTestFilesFromApi(task: Task): Map<String, TaskFile>? {
    val stepId = task.id
    if (stepId <= 0) {
      LOG.warn("Cannot load test files from API: task '${task.name}' has invalid step ID: $stepId")
      return null
    }

    LOG.info("Loading test files from API for task '${task.name}' (step $stepId)")

    // Run network request in background thread (required because this may be called from EDT)
    return try {
      ApplicationManager.getApplication().executeOnPooledThread<Map<String, TaskFile>?> {
        // ALT-10961: Use anonymous request to get original test files from API.
        // Authenticated requests return files from user's last submission instead of original stage files.
        val stepSourceResult = HyperskillConnector.getInstance().getStepSourceAnonymous(stepId)
        if (stepSourceResult is Err) {
          LOG.warn("Failed to load step source from API for step $stepId: ${stepSourceResult.error}")
          return@executeOnPooledThread null
        }

        val stepSource = (stepSourceResult as Ok).value
        val options = stepSource.block?.options as? PyCharmStepOptions
        if (options == null) {
          LOG.warn("Step $stepId has no PyCharmStepOptions")
          return@executeOnPooledThread null
        }

        val allFiles = options.files
        if (allFiles.isNullOrEmpty()) {
          LOG.warn("Step $stepId has no files in options")
          return@executeOnPooledThread null
        }

        // Filter test files (not learner-created and likely a test file by name)
        // Note: Cannot use taskFile.isTestFile here because TaskFile objects from API
        // are not attached to a Task, and isTestFile requires task context.
        val testFiles = allFiles.filter { taskFile ->
          !taskFile.isLearnerCreated && "test" in taskFile.name.lowercase()
        }

        if (testFiles.isEmpty()) {
          LOG.warn("Step $stepId has no test files")
          return@executeOnPooledThread null
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
      }.get()
    } catch (e: Exception) {
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
   */
  private fun recreateTestFiles(project: Project, taskDir: VirtualFile, task: Task) {
    // ALT-10961: Only use cached test files from API to prevent using corrupted task.taskFiles
    // Cache is populated by storeOriginalTestFiles() when fresh API data is received.
    // DO NOT update cache from task.taskFiles here - it may contain test content from another stage
    // due to framework lesson stages sharing the same task directory.
    var cachedTestFiles = originalTestFilesCache[task.id]

    if (cachedTestFiles == null) {
      LOG.warn("No cached test files for task '${task.name}' (step ${task.id}). Attempting to load from API...")
      cachedTestFiles = loadTestFilesFromApi(task)
      if (cachedTestFiles == null) {
        LOG.warn("Failed to load test files from API for task '${task.name}' (step ${task.id}). " +
                 "Skipping test files recreation to avoid using potentially corrupted task.taskFiles.")
        return
      }
    }

    val testFiles: Collection<TaskFile> = cachedTestFiles.values

    // Log test files info for diagnostics (ALT-10961)
    val filesInfo = testFiles.joinToString { "${it.name}:${it.contents.textualRepresentation.hashCode()}" }
    LOG.info("Recreating ${testFiles.size} test files for task '${task.name}' (step ${task.id}): [$filesInfo]")

    invokeAndWaitIfNeeded {
      runWriteAction {
        for (taskFile in testFiles) {
          try {
            GeneratorUtils.createChildFile(
              project.toCourseInfoHolder(),
              taskDir,
              taskFile.name,
              taskFile.contents,
              taskFile.isEditable
            )
          }
          catch (e: Exception) {
            LOG.warn("Failed to recreate test file ${taskFile.name} for task ${task.name}", e)
          }
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
    val fromNonPropagatableToPropagatableFilesState = targetPropagatableFilesState.filter { it.key in currentNonPropagatableFilesState }
    val fromPropagatableToNonPropagatableFilesState = targetNonPropagatableFilesState.filter { it.key in currentPropagatableFilesState }

    // We assume that files could not change a propagation flag from true to false (for example, from visible to invisible)
    // This behavior is not intended
    if (fromPropagatableToNonPropagatableFilesState.isNotEmpty()) {
      LOG.error("Propagation flag change from propagatable to non-propagatable during navigation in non-template-based lessons is not supported")
    }

    // Only files that do not change a propagation flag can participate in propagating user changes.
    val newCurrentPropagatableFilesState = currentPropagatableFilesState.filter { it.key !in targetNonPropagatableFilesState }
    val newTargetPropagatableFilesState = targetPropagatableFilesState.filter { it.key !in currentNonPropagatableFilesState }

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
    LOG.info("getUserChangesFromFiles: initialState=${initialState.mapValues { "${it.value.length} chars, hash=${it.value.hashCode()}" }}")
    LOG.info("getUserChangesFromFiles: diskState=${currentState.mapValues { "${it.value.length} chars, hash=${it.value.hashCode()}" }}")
    val changes = calculateChanges(initialState, currentState)
    LOG.info("getUserChangesFromFiles: calculated ${changes.changes.size} changes")
    return changes
  }

  @Synchronized
  private fun updateUserChanges(record: Int, changes: UserChanges): UpdatedUserChanges {
    return try {
      val newRecord = storage.updateUserChanges(record, changes)
      storage.force()
      UpdatedUserChanges(newRecord, changes)
    }
    catch (e: IOException) {
      LOG.error("Failed to update user changes", e)
      UpdatedUserChanges(record, UserChanges.empty())
    }
  }

  private fun getUserChangesFromStorage(task: Task): UserChanges {
    return try {
      storage.getUserChanges(task.record)
    }
    catch (e: IOException) {
      LOG.error("Failed to get user changes for task `${task.name}`", e)
      UserChanges.empty()
    }
    catch (e: NegativeArraySizeException) {
      LOG.error("Corrupted storage data detected (negative array size), recreating storage", e)
      // Storage is corrupted, recreate it completely
      try {
        Disposer.dispose(storage)
        val storageFilePath = constructStoragePath(project)
        AbstractStorage.deleteFiles(storageFilePath.toString())
        storage = createStorage(project)
        LOG.warn("Storage recreated successfully after corruption")
      }
      catch (recreateError: Exception) {
        LOG.error("Failed to recreate storage after corruption", recreateError)
      }
      UserChanges.empty()
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

    val templateFiles = task.taskFiles.filterValues { taskFile ->
      taskFile.isVisible && !taskFile.isTestFile
    }

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
    }
  }

  /**
   * Loads template files from API and caches them.
   * This is used as a fallback when the cache is empty.
   */
  private fun loadTemplateFilesFromApi(task: Task): Map<String, String>? {
    val stepId = task.id
    if (stepId <= 0) {
      LOG.warn("Cannot load template files from API: task '${task.name}' has invalid step ID: $stepId")
      return null
    }

    LOG.info("Loading template files from API for task '${task.name}' (step $stepId)")

    return try {
      ApplicationManager.getApplication().executeOnPooledThread<Map<String, String>?> {
        val stepSourceResult = HyperskillConnector.getInstance().getStepSourceAnonymous(stepId)
        if (stepSourceResult is Err) {
          LOG.warn("Failed to load step source from API for step $stepId: ${stepSourceResult.error}")
          return@executeOnPooledThread null
        }

        val stepSource = (stepSourceResult as Ok).value
        val options = stepSource.block?.options as? PyCharmStepOptions
        if (options == null) {
          LOG.warn("Step $stepId has no PyCharmStepOptions")
          return@executeOnPooledThread null
        }

        val allFiles = options.files
        if (allFiles.isNullOrEmpty()) {
          LOG.warn("Step $stepId has no files in options")
          return@executeOnPooledThread null
        }

        // Filter visible non-test files
        val templateFiles = allFiles.filter { taskFile ->
          taskFile.isVisible && !taskFile.isLearnerCreated && "test" !in taskFile.name.lowercase()
        }

        if (templateFiles.isEmpty()) {
          LOG.warn("Step $stepId has no template files")
          return@executeOnPooledThread null
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
      }.get()
    } catch (e: Exception) {
      LOG.warn("Exception while loading template files from API for step $stepId", e)
      null
    }
  }

  /**
   * Ensures template files are cached for the task.
   * If not cached, attempts to load from API.
   */
  fun ensureTemplateFilesCached(task: Task): Boolean {
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
   * Returns the original template content for visible non-test files.
   * Uses cached templates if available, otherwise loads from API.
   *
   * IMPORTANT: For framework lessons, we must use cached templates because TaskFile.contents
   * may be modified when loading submissions, which would corrupt the diff calculation.
   */
  private val Task.allFiles: FLTaskState
    get() {
      // Get list of visible non-test files
      val visibleFiles = taskFiles.filterValues { it.isVisible && !it.isTestFile }

      // Ensure templates are cached (load from API if needed)
      if (!originalTemplateFilesCache.containsKey(id)) {
        ensureTemplateFilesCached(this)
      }

      // Try to get cached templates
      val cachedTemplates = originalTemplateFilesCache[id]
      if (cachedTemplates != null) {
        // Use cached templates, but also include any new learner-created files
        val result = HashMap(cachedTemplates)
        for ((path, taskFile) in visibleFiles) {
          if (taskFile.isLearnerCreated && path !in result) {
            // New learner-created file not in cache - add it
            result[path] = taskFile.contents.textualRepresentation
          }
        }
        return result
      }

      // No cache and API failed - fall back to TaskFile.contents (may be incorrect)
      LOG.warn("No cached templates for task '${name}' (step $id), using TaskFile.contents (may be stale)")
      return visibleFiles.mapValues { it.value.contents.textualRepresentation }
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

  private fun FLTaskState.split(task: Task) = splitByKey {
    task.taskFiles[it]?.shouldBePropagated() ?: true
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
  }

  companion object {
    private val LOG: Logger = Logger.getInstance(FrameworkLessonManagerImpl::class.java)

    const val VERSION: Int = 1

    @VisibleForTesting
    fun constructStoragePath(project: Project): Path =
      Paths.get(FileUtil.join(project.basePath!!, Project.DIRECTORY_STORE_FOLDER, "frameworkLessonHistory", "storage"))

    private fun createStorage(project: Project): FrameworkStorage {
      val storageFilePath = constructStoragePath(project)

      try {
        val storage = FrameworkStorage(storageFilePath)
        storage.migrate(VERSION)
        return storage
      }
      catch (e: IllegalStateException) {
        // Storage already registered - this can happen when service is recreated during project sync
        // Delete old storage files and create fresh storage
        LOG.warn("Storage already registered at $storageFilePath, recreating", e)
        try {
          AbstractStorage.deleteFiles(storageFilePath.toString())
        }
        catch (deleteError: Exception) {
          LOG.error("Failed to delete old storage files", deleteError)
        }
        return FrameworkStorage(storageFilePath, VERSION)
      }
      catch (e: NegativeArraySizeException) {
        // Corrupted storage data - delete and recreate
        LOG.error("Storage data corrupted at $storageFilePath (negative array size), recreating", e)
        AbstractStorage.deleteFiles(storageFilePath.toString())
        return FrameworkStorage(storageFilePath, VERSION)
      }
      catch (e: IOException) {
        LOG.error("Failed to initialize storage", e)
        AbstractStorage.deleteFiles(storageFilePath.toString())
        return FrameworkStorage(storageFilePath, VERSION)
      }
    }
  }
}

private data class UpdatedUserChanges(
  val record: Int,
  val changes: UserChanges
)
