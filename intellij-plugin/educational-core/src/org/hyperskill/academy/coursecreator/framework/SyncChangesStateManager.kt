package org.hyperskill.academy.coursecreator.framework

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import com.intellij.util.ui.update.Update
import org.hyperskill.academy.learning.FileInfo
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.LessonContainer
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.framework.impl.visitFrameworkLessons
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class SyncChangesStateManager(private val project: Project) : Disposable.Default {
  private val taskFileStateStorage = ConcurrentHashMap<TaskFile, SyncChangesTaskFileState>()
  private val taskStateStorage = ConcurrentHashMap<Task, SyncChangesTaskFileState>()
  private val lessonStateStorage = ConcurrentHashMap<Lesson, SyncChangesTaskFileState>()

  fun getSyncChangesState(taskFile: TaskFile): SyncChangesTaskFileState? {
    return null
  }

  fun getSyncChangesState(task: Task): SyncChangesTaskFileState? {
    return null
  }

  fun getSyncChangesState(lesson: Lesson): SyncChangesTaskFileState? {
    return null
  }

  fun taskFileChanged(taskFile: TaskFile) = queueUpdate(taskFile)

  fun taskFileCreated(taskFile: TaskFile) = processTaskFilesCreated(taskFile.task, listOf(taskFile))

  fun filesDeleted(task: Task, taskFilesNames: List<String>) {
    // state of a current task might change from warning to info after deletion, so recalculate it
    queueUpdate(task, emptyList())

    queueSyncChangesStateForFilesInPrevTask(task, taskFilesNames)
  }

  fun taskDeleted(task: Task) = queueSyncChangesStateForFilesInPrevTask(task, null)

  fun fileMoved(file: VirtualFile, fileInfo: FileInfo.FileInTask, oldDirectoryInfo: FileInfo.FileInTask) {
  }

  fun updateSyncChangesState(lessonContainer: LessonContainer) {
    lessonContainer.visitFrameworkLessons { queueUpdate(it) }
  }

  /**
   * Removes state for given task files (that indicates there are no changes in them)
   * Does not schedule anything but update ProjectView and notifications immediately
   */
  fun removeSyncChangesState(task: Task, taskFiles: List<TaskFile>) {
  }

  fun updateSyncChangesState(task: Task) = queueUpdate(task)

  private fun collectSyncChangesState(lesson: Lesson) {
    val state = collectState(lesson.taskList) { taskStateStorage[it] }
    if (state != null) lessonStateStorage[lesson] = state
    else lessonStateStorage.remove(lesson)
  }

  private fun collectSyncChangesState(task: Task) {
    val state = collectState(task.taskFiles.values.toList()) {
      if (shouldUpdateSyncChangesState(it)) {
        taskFileStateStorage[it]
      }
      else {
        null
      }
    }
    if (state != null) taskStateStorage[task] = state
    else taskStateStorage.remove(task)
  }

  /**
   * Collects the SyncChangesTaskFileState based on the provided collect function.
   * The state represents the synchronization status of the files and will be displayed in the project view.
   */
  private fun <T> collectState(items: Iterable<T>, collect: (T) -> SyncChangesTaskFileState?): SyncChangesTaskFileState? {
    var resultState: SyncChangesTaskFileState? = null
    for (item in items) {
      val state = collect(item) ?: continue
      if (state == SyncChangesTaskFileState.WARNING) return SyncChangesTaskFileState.WARNING
      resultState = SyncChangesTaskFileState.INFO
    }
    return resultState
  }

  private fun refreshUI() {
    ProjectView.getInstance(project).refresh()
    EditorNotifications.updateAll()
  }

  // In addition/deletion of files, framework lesson structure might break/restore,
  // so we need to recalculate the state for corresponding task files from a previous task
  // in case when a warning state is added/removed
  private fun processTaskFilesCreated(task: Task, taskFiles: List<TaskFile>) {
    queueUpdate(task, taskFiles)
    queueSyncChangesStateForFilesInPrevTask(task, taskFiles.map { it.name })
  }

  private fun queueUpdate(taskFile: TaskFile) = queueUpdate(taskFile.task, listOf(taskFile))
  private fun queueUpdate(task: Task) = queueUpdate(task, task.taskFiles.values.toList())

  private fun queueUpdate(task: Task, taskFiles: List<TaskFile>) {
    return
  }

  private fun queueUpdate(lesson: Lesson) {
    return
  }

  // Process a batch of taskFiles in a certain task at once to minimize the number of accesses to the storage
  private fun recalcSyncChangesState(task: Task, taskFiles: List<TaskFile>) {
    for (taskFile in taskFiles) {
      taskFileStateStorage.remove(taskFile)
    }

    val updatableTaskFiles = taskFiles.filter { shouldUpdateSyncChangesState(it) }

    val (warningTaskFiles, otherTaskFiles) = updatableTaskFiles.partition { checkForAbsenceInNextTask(it) }

    for (taskFile in warningTaskFiles) {
      taskFileStateStorage[taskFile] = SyncChangesTaskFileState.WARNING
    }

    val changedTaskFiles = CCFrameworkLessonManager.getInstance(project).getChangedFiles(task)
    val infoTaskFiles = otherTaskFiles.intersect(changedTaskFiles.toSet())

    for (taskFile in infoTaskFiles) {
      taskFileStateStorage[taskFile] = SyncChangesTaskFileState.INFO
    }
  }

  // do not update state for the last framework lesson task and for non-propagatable files (invisible files)
  private fun shouldUpdateSyncChangesState(taskFile: TaskFile): Boolean {
    val task = taskFile.task
    return taskFile.isPropagatable && task.lesson.taskList.last() != task
  }

  // after deletion of files, the framework lesson structure might break,
  // so we need to recalculate state for a corresponding file from a previous task in case when a warning state is added/removed
  private fun queueSyncChangesStateForFilesInPrevTask(task: Task, filterTaskFileNames: List<String>?) {
    val prevTask = task.lesson.taskList.getOrNull(task.index - 2) ?: return
    if (filterTaskFileNames == null) {
      queueUpdate(prevTask)
      return
    }
    val correspondingTaskFiles = prevTask.taskFiles.filter { it.key in filterTaskFileNames }.values.toList()
    queueUpdate(prevTask, correspondingTaskFiles)
  }

  private fun checkForAbsenceInNextTask(taskFile: TaskFile): Boolean {
    val task = taskFile.task
    val nextTask = task.lesson.taskList.getOrNull(task.index) ?: return false
    return taskFile.name !in nextTask.taskFiles
  }


  /**
   * Base class for sync changes updates.
   */
  private sealed class SyncChangesUpdate(priority: Int) : Update(Any(), false, priority)

  /**
   * Base class for updating sync changes state for a given study item
   * Lower priority, since all task files updates must be executed earlier than study item state updates
   */
  private sealed class StudyItemSyncChangesUpdate<T : StudyItem>(priority: Int, val item: T) : SyncChangesUpdate(priority)

  /**
   * Class for updating state for a given task
   */
  private inner class TaskSyncChangesUpdate(task: Task) : StudyItemSyncChangesUpdate<Task>(LOW_PRIORITY, task) {
    override fun canEat(update: Update): Boolean {
      if (super.canEat(update)) return true
      if (update !is TaskSyncChangesUpdate) return false
      return item == update.item
    }

    override fun run() {
      collectSyncChangesState(item)
    }
  }

  companion object {
    fun getInstance(project: Project): SyncChangesStateManager = project.service()
  }
}
