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

  /**
   * Sections and lessons created locally (Hyperskill topics) never receive a server id, so several of them share
   * id `0`. Matching purely by id then pairs the first id-less remote item with every id-less local item and turns
   * all the remaining ones into deletions, which erase the learner's files. Names disambiguate those.
   */
  protected fun <I : StudyItem> Collection<I>.findCounterpartOf(localItem: I): I? {
    firstOrNull { it.id != 0 && it.id == localItem.id }?.let { return it }
    if (localItem.id != 0) return null

    val idLessItems = filter { it.id == 0 }
    // `singleOrNull` keeps a renamed item paired with its counterpart while it is the only candidate
    return idLessItems.firstOrNull { it.name == localItem.name } ?: idLessItems.singleOrNull()
  }

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