package org.hyperskill.academy.learning.update

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.actions.EduActionUtils.getCurrentTask
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.*
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.UnsupportedTask
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.framework.FrameworkLessonManager
import org.hyperskill.academy.learning.framework.propagateFilesOnNavigation
import org.hyperskill.academy.learning.invokeLater
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.toCourseInfoHolder

object UpdateUtils {
  private val LOG = Logger.getInstance(UpdateUtils::class.java)

  fun updateTaskDescription(project: Project, task: Task, remoteTask: Task) {
    LOG.info("updateTaskDescription: localTask=${task.javaClass.simpleName} '${task.name}', remoteTask=${remoteTask.javaClass.simpleName} '${remoteTask.name}'")

    // Don't update description if remote task is UnsupportedTask and local task is not
    // This prevents overwriting valid descriptions with "tasks are not supported yet" message
    if (remoteTask is UnsupportedTask && task !is UnsupportedTask) {
      LOG.info("Skipping description update: remote is UnsupportedTask, local is ${task.javaClass.simpleName}")
      return
    }

    LOG.info("Updating description from remote task")
    task.descriptionText = remoteTask.descriptionText
    task.descriptionFormat = remoteTask.descriptionFormat
    task.feedbackLink = remoteTask.feedbackLink

    // Task Description file needs to be regenerated as it already exists
    val taskDir = task.getTaskDirectory(project)
    if (taskDir != null) {
      GeneratorUtils.createDescriptionFile(project, taskDir, task)
    }
  }

  fun updateFrameworkLessonFiles(
    project: Project,
    lesson: FrameworkLesson,
    task: Task,
    remoteTask: Task,
    updatePropagatableFiles: Boolean
  ) {
    LOG.warn("UPDATE_FRAMEWORK_FILES: task='${task.name}' (id=${task.id}, status=${task.status}), " +
             "lessonCurrentIndex=${lesson.currentTaskIndex}, taskIndex=${task.index}, " +
             "updatePropagatableFiles=$updatePropagatableFiles, hasChangedFiles=${task.hasChangedFiles(project)}")
    fun updateTaskFiles(
      task: Task,
      remoteTaskFiles: Map<String, TaskFile>,
      updateInLocalFS: Boolean
    ) {
      for ((path, remoteTaskFile) in remoteTaskFiles) {
        val taskFile = task.taskFiles[path]
        val currentTaskFile = if (taskFile != null) {
          taskFile.contents = remoteTaskFile.contents
          taskFile
        }
        else {
          task.addTaskFile(remoteTaskFile)
          remoteTaskFile
        }

        if (updateInLocalFS) {
          // remove read only flags, so we can write new content to non-editable files
          // editable flags for task files will be restored in [GeneratorUtils.createChildFile()] call
          removeReadOnlyFlags(project, currentTaskFile)

          val taskDir = task.getDir(project.courseDir)
          if (taskDir != null) {
            GeneratorUtils.createChildFile(
              project.toCourseInfoHolder(),
              taskDir,
              path,
              currentTaskFile.contents,
              currentTaskFile.isEditable
            )
          }
        }
      }

      task.init(lesson, false)
    }

    val flm = FrameworkLessonManager.getInstance(project)

    // Find new propagatable files added by author (exist in remote but not in local)
    val newPropagatableFiles = remoteTask.taskFiles.filter { (path, taskFile) ->
      path !in task.taskFiles && taskFile.shouldBePropagated()
    }
    if (newPropagatableFiles.isNotEmpty()) {
      LOG.info("Found ${newPropagatableFiles.size} new template files from server: ${newPropagatableFiles.keys}")
    }

    val isCurrentTask = lesson.currentTaskIndex == task.index - 1

    if (!isCurrentTask) {
      updateTaskFiles(task, remoteTask.nonPropagatableFiles, false)
      // Add new propagatable files to model (not to disk - will be written when navigating)
      if (newPropagatableFiles.isNotEmpty()) {
        updateTaskFiles(task, newPropagatableFiles, false)
        // Update storage snapshot with new files
        flm.addNewFilesToSnapshot(task, newPropagatableFiles.mapValues { it.value.contents.textualRepresentation })
      }
      flm.updateUserChanges(task, task.taskFiles.mapValues { (_, taskFile) -> taskFile.contents.textualRepresentation })
    }
    else {
      if (updatePropagatableFiles && !task.hasChangedFiles(project)) {
        updateTaskFiles(task, remoteTask.taskFiles, true)
      }
      else {
        updateTaskFiles(task, remoteTask.nonPropagatableFiles, true)
        // Add new propagatable files to disk and model for current task
        if (newPropagatableFiles.isNotEmpty()) {
          updateTaskFiles(task, newPropagatableFiles, true)
          // Storage will be updated on next auto-save
        }
      }
    }

    // Propagate new files to all subsequent stages (only in non-template-based mode)
    // This mimics what would happen if files existed from the beginning
    if (newPropagatableFiles.isNotEmpty() && lesson.propagateFilesOnNavigation) {
      propagateNewFilesToSubsequentStages(lesson, task, newPropagatableFiles, flm)
    }

    // Update the test and template files caches after updating task files from remote.
    // This ensures the caches reflect the updated content (ALT-10961).
    // Use update* methods to force-update the cache (store* methods won't overwrite).
    flm.updateOriginalTestFiles(task)
    flm.updateOriginalTemplateFiles(task)

    // Update storage snapshot with new test files from server.
    // This ensures navigation uses the updated test files.
    flm.updateSnapshotTestFiles(task)
  }

  private val Task.nonPropagatableFiles: Map<String, TaskFile>
    get() = taskFiles.filter { !it.value.shouldBePropagated() }

  /**
   * Propagates new template files to all subsequent stages in the lesson.
   * This mimics what would happen if files existed from the beginning -
   * they would naturally propagate through all stages.
   *
   * @param lesson the framework lesson
   * @param sourceTask the task where new files were added
   * @param newFiles map of new propagatable files
   * @param flm the framework lesson manager
   */
  private fun propagateNewFilesToSubsequentStages(
    lesson: FrameworkLesson,
    sourceTask: Task,
    newFiles: Map<String, TaskFile>,
    flm: FrameworkLessonManager
  ) {
    val sourceTaskIndex = sourceTask.index
    val subsequentTasks = lesson.taskList.filter { it.index > sourceTaskIndex }

    if (subsequentTasks.isEmpty()) {
      LOG.info("No subsequent stages to propagate new files to")
      return
    }

    LOG.info("Propagating ${newFiles.size} new files to ${subsequentTasks.size} subsequent stages")

    val newFilesContent = newFiles.mapValues { it.value.contents.textualRepresentation }

    for (subsequentTask in subsequentTasks) {
      // Add files to task model (only if not already present)
      var addedToModel = 0
      for ((path, taskFile) in newFiles) {
        if (path !in subsequentTask.taskFiles) {
          // Create a copy of TaskFile for each subsequent task
          val copiedTaskFile = TaskFile(taskFile.name, taskFile.contents).also {
            it.isVisible = taskFile.isVisible
            it.isEditable = taskFile.isEditable
            it.isLearnerCreated = taskFile.isLearnerCreated
          }
          subsequentTask.addTaskFile(copiedTaskFile)
          addedToModel++
        }
      }

      // Add files to storage snapshot (only if snapshot exists)
      flm.addNewFilesToSnapshot(subsequentTask, newFilesContent)

      if (addedToModel > 0) {
        LOG.info("Propagated $addedToModel new files to stage '${subsequentTask.name}'")
      }
    }
  }

  private fun removeReadOnlyFlags(project: Project, taskFile: TaskFile) {
    val virtualTaskFile = taskFile.getVirtualFile(project) ?: return
    invokeAndWaitIfNeeded {
      runWriteAction {
        GeneratorUtils.removeNonEditableFileFromCourse(taskFile.course(), virtualTaskFile)
      }
    }
  }

  fun FrameworkLesson.shouldFrameworkLessonBeUpdated(lessonFromServer: FrameworkLesson): Boolean {
    val tasksFromServer = lessonFromServer.taskList
    val localTasks = taskList
    return when {
      localTasks.size > tasksFromServer.size -> false
      localTasks.zip(tasksFromServer).any { (task, remoteTask) -> task.id != remoteTask.id } -> false
      else -> true
    }
  }

  fun navigateToTaskAfterUpdate(project: Project) {
    project.invokeLater {
      val currentTask = project.getCurrentTask()
      val course = project.course ?: return@invokeLater

      if (currentTask != null) {
        NavigationUtils.navigateToTask(project, currentTask)
      }
      else {
        NavigationUtils.openFirstTask(course, project)
      }
    }
  }
}