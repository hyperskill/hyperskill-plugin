package org.hyperskill.academy.learning.update.elements

import com.intellij.openapi.application.EDT
import com.intellij.openapi.application.writeAction
import com.intellij.openapi.progress.blockingContext
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.ItemContainer
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.update.StudyItemUpdater.Companion.deleteFilesOnDisc
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer

sealed class LessonUpdate(localItem: Lesson?, remoteItem: Lesson?) : StudyItemUpdate<Lesson>(localItem, remoteItem)

data class LessonCreationInfo(
  val localContainer: ItemContainer,
  override val remoteItem: Lesson
) : LessonUpdate(null, remoteItem) {
  override suspend fun update(project: Project) {
    localContainer.addItem(remoteItem)
    remoteItem.init(localContainer, false)

    val parentDir = localContainer.getDir(project.courseDir) ?: error("Failed to find parent dir: ${localContainer.name}")
    withContext(Dispatchers.IO) {
      blockingContext {
        GeneratorUtils.createLesson(project, remoteItem, parentDir)
      }
    }

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(remoteItem)
    }
  }
}

data class LessonUpdateInfo(
  override val localItem: Lesson,
  override val remoteItem: Lesson,
  val taskUpdates: List<TaskUpdate>
) : LessonUpdate(localItem, remoteItem) {
  override suspend fun update(project: Project) {
    if (localItem is FrameworkLesson) {
      ensureFrameworkLessonCurrentTaskIsNotDeleted(project, localItem, remoteItem.taskList.lastIndex)
    }

    if (taskUpdates.isNotEmpty()) {
      taskUpdates.forEach {
        it.update(project)
      }
      localItem.sortItems()
    }

    localItem.index = remoteItem.index

    if (localItem.name != remoteItem.name) {
      val courseDir = project.courseDir
      val fromDir = localItem.getDir(courseDir) ?: error("Lesson dir wasn't found")
      val parentDir = localItem.parent.getDir(courseDir) ?: error("Parent dir wasn't found")

      localItem.name = remoteItem.name
      withContext(Dispatchers.IO) {
        val toDir = blockingContext { GeneratorUtils.createUniqueDir(parentDir, localItem) }
        writeAction {
          fromDir.children.forEach { it.move(this, toDir) }
          fromDir.delete(this)
        }
      }
    }

    blockingContext {
      YamlFormatSynchronizer.saveItemWithRemoteInfo(localItem)
    }
  }

  private suspend fun ensureFrameworkLessonCurrentTaskIsNotDeleted(
    project: Project,
    lesson: FrameworkLesson,
    lastTaskIndexInRemoteLesson: Int
  ) {
    if (lesson.currentTaskIndex <= lastTaskIndexInRemoteLesson) return

    val currentTask = lesson.currentTask() ?: return
    val lastNonDeletedTask = lesson.taskList.getOrNull(lastTaskIndexInRemoteLesson) ?: return

    // We explicitly navigate to the last non-deleted task to properly update the state of the framework lesson
    withContext(Dispatchers.EDT) {
      blockingContext {
        NavigationUtils.prepareFilesForTargetTask(project, lesson, currentTask, lastNonDeletedTask, showDialogIfConflict = false)
      }
    }
  }
}

data class LessonDeletionInfo(override val localItem: Lesson) : LessonUpdate(localItem, null) {
  override suspend fun update(project: Project) {
    val parentContainer = localItem.parent
    parentContainer.removeItem(localItem)
    localItem.deleteFilesOnDisc(project)
  }
}