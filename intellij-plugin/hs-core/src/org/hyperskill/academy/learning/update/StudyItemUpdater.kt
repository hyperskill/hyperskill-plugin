package org.hyperskill.academy.learning.update

import com.intellij.openapi.application.writeAction
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.ext.getTaskDirectory
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.update.elements.StudyItemUpdate
import org.jetbrains.annotations.TestOnly

// TODO EDU-5830 maybe synchronization is needed
abstract class StudyItemUpdater<T : StudyItem, U : StudyItemUpdate<T>>(protected val project: Project) : ItemUpdater<T> {
  protected abstract suspend fun collect(localItems: List<T>, remoteItems: List<T>): List<U>

  @TestOnly
  protected suspend fun update(localItems: List<T>, remoteItems: List<T>) {
    val updates = collect(localItems, remoteItems)
    updates.forEach {
      it.update(project)
    }
  }

  companion object {
    @Suppress("UnstableApiUsage")
    suspend fun <T : StudyItem> T.deleteFilesOnDisc(project: Project) {
      val virtualFile = when (this) {
        is Task -> getTaskDirectory(project) ?: return
        else -> getDir(project.courseDir) ?: return
      }
      writeAction {
        virtualFile.delete(this::class.java)
      }
    }
  }
}