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

    if (lesson.currentTaskIndex != task.index - 1) {
      updateTaskFiles(task, remoteTask.nonPropagatableFiles, false)
      flm.updateUserChanges(task, task.taskFiles.mapValues { (_, taskFile) -> taskFile.contents.textualRepresentation })
    }
    else {
      if (updatePropagatableFiles && !task.hasChangedFiles(project)) {
        updateTaskFiles(task, remoteTask.taskFiles, true)
      }
      else {
        updateTaskFiles(task, remoteTask.nonPropagatableFiles, true)
      }
    }

    // Update the test files cache after updating task files from remote.
    // This ensures the cache reflects the updated test files content (ALT-10961).
    // Use updateOriginalTestFiles to force-update the cache (storeOriginalTestFiles won't overwrite).
    flm.updateOriginalTestFiles(task)
  }

  private val Task.nonPropagatableFiles: Map<String, TaskFile>
    get() = taskFiles.filter { !it.value.shouldBePropagated() }

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