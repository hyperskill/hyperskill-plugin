package org.hyperskill.academy.learning.update

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.readAction
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.ext.getDescriptionFile
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.getTextFromTaskTextFile
import org.hyperskill.academy.learning.update.elements.TaskUpdate

abstract class TaskUpdaterBase<T : Lesson>(project: Project, protected val lesson: T) : StudyItemUpdater<Task, TaskUpdate>(project) {

  suspend fun collect(remoteLesson: Lesson): List<TaskUpdate> = collect(lesson.taskList, remoteLesson.taskList)

  protected suspend fun Task.isChanged(remoteTask: Task): Boolean {
    val newTaskFiles = remoteTask.taskFiles
    val taskDescriptionText = descriptionText.ifEmpty {
      withContext(Dispatchers.EDT) {
        readAction { getDescriptionFile(project)?.getTextFromTaskTextFile() ?: "" }
      }
    }

    return when {
      name != remoteTask.name -> true
      index != remoteTask.index -> true
      taskFiles.size != newTaskFiles.size -> true
      taskDescriptionText != remoteTask.descriptionText -> true
      descriptionFormat != remoteTask.descriptionFormat -> true
      javaClass != remoteTask.javaClass -> true

      this is RemoteEduTask && remoteTask is RemoteEduTask -> {
        checkProfile != remoteTask.checkProfile
      }

      else -> {
        newTaskFiles.any { (newFileName, newTaskFile) ->
          isTaskFileChanged(taskFiles[newFileName] ?: return@any true, newTaskFile)
        }
      }
    }
  }

  private fun isTaskFileChanged(taskFile: TaskFile, newTaskFile: TaskFile): Boolean {
    return taskFile.contents.textualRepresentation != newTaskFile.contents.textualRepresentation
  }
}