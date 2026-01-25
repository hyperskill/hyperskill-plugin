package org.hyperskill.academy.learning

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.application.runUndoTransparentWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotifications
import org.hyperskill.academy.learning.EduUtilsKt.isNewlyCreated
import org.hyperskill.academy.learning.actions.EduActionUtils
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.InMemoryTextualContents
import org.hyperskill.academy.learning.courseFormat.ext.*
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.submissions.Submission
import org.hyperskill.academy.learning.submissions.SubmissionSettings
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import org.hyperskill.academy.learning.submissions.isSignificantlyAfter
import org.hyperskill.academy.learning.update.showUpdateNotification
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.VisibleForTesting
import java.util.*
import java.util.Collections.max
import java.util.concurrent.Future
import kotlin.math.max

abstract class SolutionLoaderBase(protected val project: Project) : Disposable {

  private var futures: Map<Int, Future<Boolean>> = HashMap()

  open fun loadSolutionsInBackground() {
    val course = StudyTaskManager.getInstance(project).course ?: return
    ProgressManager.getInstance().run(object : Backgroundable(project, EduCoreBundle.message("update.loading.submissions")) {
      override fun run(progressIndicator: ProgressIndicator) {
        loadAndApplySolutions(course, progressIndicator)
      }
    })
  }

  fun loadSolutionsInBackground(course: Course, tasksToUpdate: List<Task>, force: Boolean) {
    ProgressManager.getInstance().run(object : Backgroundable(project, EduCoreBundle.message("update.loading.submissions")) {
      override fun run(progressIndicator: ProgressIndicator) {
        loadAndApplySolutions(course, tasksToUpdate, progressIndicator, force)
      }
    })
  }

  @VisibleForTesting
  fun loadAndApplySolutions(course: Course, progressIndicator: ProgressIndicator? = null) {
    loadAndApplySolutions(course, course.allTasks, progressIndicator)
  }

  private fun loadAndApplySolutions(
    course: Course,
    tasksToUpdate: List<Task>,
    progressIndicator: ProgressIndicator?,
    force: Boolean = false
  ) {
    val submissions = runWithProgress(progressIndicator) { loadSubmissions(tasksToUpdate) }

    progressIndicator?.text = EduCoreBundle.message("update.updating.tasks")
    if (submissions.isNotEmpty() || SubmissionSettings.getInstance(project).stateOnClose) {
      return updateTasks(course, tasksToUpdate, submissions, progressIndicator, force)
    }
    LOG.warn("Can't get submissions")
  }

  private fun <T> runWithProgress(progressIndicator: ProgressIndicator?, doLoad: () -> T): T {
    return if (progressIndicator != null) {
      ApplicationUtil.runWithCheckCanceled(doLoad, progressIndicator)
    }
    else {
      doLoad()
    }
  }

  protected open fun updateTasks(
    course: Course,
    tasks: List<Task>,
    submissions: List<Submission>,
    progressIndicator: ProgressIndicator?,
    force: Boolean = false
  ) {
    progressIndicator?.isIndeterminate = false
    cancelUnfinishedTasks()
    val tasksToUpdate = tasks.filter { task -> task.hasSolutions() }
    var finishedTaskCount = 0
    val futures = HashMap<Int, Future<Boolean>>(tasks.size)
    for (task in tasksToUpdate) {
      invokeAndWaitIfNeeded {
        if (project.isDisposed) return@invokeAndWaitIfNeeded
        for (file in getOpenFiles(project, task)) {
          file.startLoading(project)
        }
      }
      futures[task.id] = ApplicationManager.getApplication().executeOnPooledThread<Boolean> {
        try {
          ProgressManager.checkCanceled()
          updateTask(project, task, submissions, force)
        }
        finally {
          if (progressIndicator != null) {
            synchronized(progressIndicator) {
              finishedTaskCount++
              progressIndicator.fraction = finishedTaskCount.toDouble() / tasksToUpdate.size
              progressIndicator.text = EduCoreBundle.message("loading.solution.progress", finishedTaskCount, tasksToUpdate.size)
            }
          }
          project.invokeLater {
            for (file in getOpenFiles(project, task)) {
              file.stopLoading(project)
              EditorNotifications.getInstance(project).updateNotifications(file)
            }
          }
        }
      }
    }

    synchronized(this) {
      this.futures = futures
    }

    ProgressManager.checkCanceled()
    val connection = project.messageBus.connect()
    connection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, object : FileEditorManagerListener {
      override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        val task = file.getContainingTask(project) ?: return
        val future = futures[task.id] ?: return
        if (!future.isDone) {
          file.startLoading(project)
        }
      }
    })

    try {
      waitAllTasks(futures.values)
    }
    finally {
      connection.disconnect()
    }

    val needToShowNotification = needToShowUpdateNotification(futures.values)
    runInEdt {
      if (project.isDisposed) return@runInEdt
      if (needToShowNotification && !RemoteEnvHelper.isRemoteDevServer()) {
        // Suppression is needed here because DialogTitleCapitalization is demanded by the superclass constructor,
        // but the plugin naming with the capital letters used in the notification title
        @Suppress("DialogTitleCapitalization")
        showUpdateNotification(
          project,
          EduCoreBundle.message("notification.update.plugin.title"),
          EduCoreBundle.message("notification.update.plugin.apply.solutions.content")
        )
      }
      EduUtilsKt.synchronize()
      ProjectView.getInstance(project).refresh()
    }
  }

  private fun getOpenFiles(project: Project, task: Task): List<VirtualFile> {
    return FileEditorManager.getInstance(project).openFiles.filter {
      it.getTaskFile(project)?.task == task
    }
  }

  private fun waitAllTasks(tasks: Collection<Future<*>>) {
    for (task in tasks) {
      if (isUnitTestMode) {
        EduActionUtils.waitAndDispatchInvocationEvents(task)
      }
      else {
        try {
          task.get()
        }
        catch (e: Exception) {
          LOG.warn(e)
        }
      }
    }
  }

  private fun needToShowUpdateNotification(tasks: Collection<Future<*>>): Boolean {
    return tasks.any { future ->
      try {
        future.get() == true
      }
      catch (e: Exception) {
        LOG.warn(e)
        false
      }
    }
  }

  @Synchronized
  private fun cancelUnfinishedTasks() {
    for (future in futures.values) {
      if (!future.isDone) {
        future.cancel(true)
      }
    }
  }

  /**
   * @return true if solutions for given task are incompatible with current plugin version, false otherwise
   */
  open fun updateTask(project: Project, task: Task, submissions: List<Submission>, force: Boolean = false): Boolean {
    val taskSolutions = loadSolution(task, submissions)
    ProgressManager.checkCanceled()

    if (taskSolutions.solutions.isNotEmpty()) {
      if (!taskSolutions.hasIncompatibleSolutions) {
        applySolutions(project, task, taskSolutions, force)
      }
    }
    else {
      if (taskSolutions.checkStatus != CheckStatus.Unchecked) {
        applyCheckStatus(task, taskSolutions.checkStatus)
      }
    }
    return taskSolutions.hasIncompatibleSolutions
  }

  private fun applyCheckStatus(task: Task, checkStatus: CheckStatus) {
    task.status = checkStatus
    YamlFormatSynchronizer.saveItem(task)
  }

  override fun dispose() {
    cancelUnfinishedTasks()
  }

  protected open fun loadSubmissions(tasks: List<Task>): List<Submission> =
    SubmissionsManager.getInstance(project).getOrLoadSubmissions(tasks)

  protected abstract fun loadSolution(task: Task, submissions: List<Submission>): TaskSolutions

  companion object {

    private val LOG = Logger.getInstance(SolutionLoaderBase::class.java)

    private fun Task.modificationDate(project: Project): Date {
      val lesson = lesson
      return if (lesson is FrameworkLesson && lesson.currentTask() != this) {
        val timestamp = FrameworkLessonManager.getInstance(project).getChangesTimestamp(this)
        Date(max(0, timestamp))
      }
      else {
        val taskDir = getDir(project.courseDir) ?: return Date(0)
        Date(max(taskFiles.values.map { it.findTaskFileInDir(taskDir)?.timeStamp ?: 0 }))
      }
    }

    private fun Task.modifiedBefore(project: Project, taskSolutions: TaskSolutions): Boolean {
      val solutionDate = taskSolutions.date ?: return true
      val localTaskModificationDate = modificationDate(project)
      return solutionDate.isSignificantlyAfter(localTaskModificationDate)
    }

    private fun applySolutions(
      project: Project,
      task: Task,
      taskSolutions: TaskSolutions,
      force: Boolean
    ) {
      project.invokeLater {
        task.status = taskSolutions.checkStatus
        YamlFormatSynchronizer.saveItem(task)
        val lesson = task.lesson
        if (lesson is FrameworkLesson && lesson.currentTask() != task) {
          if (force || task.modifiedBefore(project, taskSolutions)) {
            applySolutionToNonCurrentTask(project, task, taskSolutions)
          }
        }
        else {
          if (force || project.isNewlyCreated() || task.modifiedBefore(project, taskSolutions)) {
            applySolutionToCurrentTask(project, task, taskSolutions)
          }
        }
      }
    }

    private fun applySolutionToNonCurrentTask(project: Project, task: Task, taskSolutions: TaskSolutions) {
      val frameworkLessonManager = FrameworkLessonManager.getInstance(project)

      // ALT-10961: Ensure template files are cached before saving external changes.
      // Use ensureTemplateFilesCached which loads from API if cache is empty.
      // storeOriginalTemplateFiles uses task.taskFiles which may have stale disk content.
      frameworkLessonManager.ensureTemplateFilesCached(task)

      val solutionMap = taskSolutions.solutions.mapValues { it.value.text }
      frameworkLessonManager.saveExternalChanges(task, solutionMap, taskSolutions.submissionId)
      for (taskFile in task.taskFiles.values) {
        val solution = taskSolutions.solutions[taskFile.name] ?: continue

        taskFile.isVisible = solution.isVisible
      }
    }

    private fun applySolutionToCurrentTask(project: Project, task: Task, taskSolutions: TaskSolutions) {
      val taskDir = task.getDir(project.courseDir) ?: error("Directory for task `${task.name}` not found")
      for ((path, solution) in taskSolutions.solutions) {
        val taskFile = task.getTaskFile(path)

        // Skip test files from submissions to prevent corrupted tests from being applied
        // Test files should always come from step source (API), not from user submissions
        // See ALT-10961: user submissions may contain stale test files from previous stages
        if (taskFile != null && !taskFile.isLearnerCreated && taskFile.isTestFile) {
          LOG.warn("Skipping test file '$path' from submission for task '${task.name}' - test files should come from API, not submissions")
          continue
        }

        if (taskFile == null) {
          GeneratorUtils.createChildFile(project, taskDir, path, InMemoryTextualContents(solution.text))
          val createdFile = task.getTaskFile(path)
          if (createdFile == null) {
            val help = if (isUnitTestMode) "Don't you forget to use `withVirtualFileListener`?" else ""
            LOG.error("taskFile $path should be created moment ago. $help")
            continue
          }
          createdFile.isVisible = solution.isVisible
        }
        else {
          val vFile = taskDir.findFileByRelativePath(path) ?: continue
          taskFile.isVisible = solution.isVisible

          if (!taskFile.isVisible) continue

          EduDocumentListener.modifyWithoutListener(task, path) {
            runUndoTransparentWriteAction {
              val document = FileDocumentManager.getInstance().getDocument(vFile) ?: error("No document for $path")
              document.setText(solution.text)
            }
          }
        }
      }

      // ALT-10961: Also save the current task's solution to storage.
      // This ensures that when the user navigates away and back, the submission content
      // is preserved instead of being replaced with the template.
      val lesson = task.lesson
      if (lesson is FrameworkLesson) {
        val frameworkLessonManager = FrameworkLessonManager.getInstance(project)
        val solutionMap = taskSolutions.solutions.mapValues { it.value.text }
        frameworkLessonManager.saveExternalChanges(task, solutionMap, taskSolutions.submissionId)
      }
    }
  }

  protected data class Solution(val text: String, val isVisible: Boolean)

  protected class TaskSolutions(
    val date: Date?,
    val checkStatus: CheckStatus,
    val solutions: Map<String, Solution> = emptyMap(),
    val hasIncompatibleSolutions: Boolean = false,
    val submissionId: Long? = null
  ) {
    companion object {
      val EMPTY = TaskSolutions(null, CheckStatus.Unchecked)
    }
  }
}
