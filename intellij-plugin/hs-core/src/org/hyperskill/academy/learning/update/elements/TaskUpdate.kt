package org.hyperskill.academy.learning.update.elements

import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hyperskill.academy.learning.EduCourseUpdater
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.update.StudyItemUpdater.Companion.deleteFilesOnDisc
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer

sealed class TaskUpdate(localItem: Task?, remoteItem: Task?) : StudyItemUpdate<Task>(localItem, remoteItem)

data class TaskCreationInfo(val localLesson: Lesson, override val remoteItem: Task) : TaskUpdate(null, remoteItem) {
  override suspend fun update(project: Project) {
    localLesson.addItem(remoteItem)
    remoteItem.init(localLesson, false)

    val lessonDir = localLesson.getDir(project.courseDir) ?: error("Failed to find lesson dir: ${localLesson.name}")
    withContext(Dispatchers.IO) {
      GeneratorUtils.createTask(project, remoteItem, lessonDir)
    }

    YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
  }
}

data class TaskUpdateInfo(override val localItem: Task, override val remoteItem: Task) : TaskUpdate(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    val lesson = localItem.parent

    lesson.removeItem(localItem)
    localItem.deleteFilesOnDisc(project)

    remoteItem.apply {
      // we keep CheckStatus.Solved for task even if it was updated
      // we keep CheckStatus.Failed for task only if it was not updated
      if (localItem.status != CheckStatus.Failed) {
        status = localItem.status
      }
      init(lesson, false)
    }
    val lessonDir = lesson.getDir(project.courseDir) ?: error("Lesson dir wasn't found")
    withContext(Dispatchers.IO) {
      EduCourseUpdater.createTaskDirectories(project, lessonDir, remoteItem)
    }
    lesson.addItem(remoteItem)

    YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
  }
}

data class TaskDeletionInfo(override val localItem: Task) : TaskUpdate(localItem, null) {
  override suspend fun update(project: Project) {
    val lesson = localItem.parent
    lesson.removeItem(localItem)
    localItem.deleteFilesOnDisc(project)
  }
}