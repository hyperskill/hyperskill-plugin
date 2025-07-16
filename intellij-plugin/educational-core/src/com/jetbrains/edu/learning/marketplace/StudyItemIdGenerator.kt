package com.jetbrains.edu.learning.marketplace

import com.intellij.openapi.application.invokeAndWaitIfNeeded
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresBlockingContext
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.ext.visitItems
import com.jetbrains.edu.learning.yaml.YamlDeepLoader.loadRemoteInfoRecursively
import com.jetbrains.edu.learning.yaml.YamlFormatSynchronizer
import com.jetbrains.edu.learning.yaml.errorHandling.RemoteYamlLoadingException
import org.jetbrains.annotations.VisibleForTesting
import kotlin.random.Random

typealias DuplicateIdMap = Map<Int, List<StudyItem>>

@Service(Service.Level.PROJECT)
class StudyItemIdGenerator(private val project: Project) {

  /**
   * Generates ids for all study items in given [course] if they are not assigned yet (i.e. [StudyItem.id] equals 0)
   */
  @RequiresBlockingContext
  @Throws(RemoteYamlLoadingException::class)
  fun generateIdsIfNeeded(course: Course) {
    // Load `*-remote-info.yaml` files for each item to have up-to-date ids
    course.loadRemoteInfoRecursively(project)
    generateMissingIds(course)
    // Dump info about new ids to `*-remote-info.yaml` files
    YamlFormatSynchronizer.saveRemoteInfo(course)
  }

  fun collectItemsWithDuplicateIds(course: Course): DuplicateIdMap {
    val idToItems = hashMapOf<Int, MutableList<StudyItem>>()
    course.visitItems { item ->
      // item doesn't have remote id
      if (item.id == 0) return@visitItems

      val items = idToItems.getOrPut(item.id) { mutableListOf() }
      items += item
    }

    return idToItems.filterValues { it.size > 1 }
  }

  /**
   * Generates ids for all study items in given [course] if they are not assigned yet (i.e. [StudyItem.id] equals 0).
   *
   * [bannedIds] cannot be used as new ids even if there isn't any study item with such ids.
   * They are supposed to be used when we need to regenerate ids for some items, and we don't want to use old values
   */
  @RequiresBlockingContext
  private fun generateMissingIds(
    course: Course,
    items: List<StudyItem> = course.allItems,
    bannedIds: Set<Int> = emptySet()
  ) {
    // Generate missing ids
    val updates = hashMapOf<StudyItem, Int>()
    val usedIds = collectExistingIds(course)
    usedIds += bannedIds

    for (item in items) {
      if (item.id != 0) continue
      var newId: Int
      do {
        newId = generateNewId()
      }
      while (!usedIds.add(newId))

      updates[item] = newId
    }

    // Save new ids to the corresponding study items
    invokeAndWaitIfNeeded {
      runWriteAction {
        for ((item, id) in updates) {
          item.id = id
        }
      }
    }
  }

  @VisibleForTesting
  fun generateNewId(): Int = Random.Default.nextInt(1, Int.MAX_VALUE)

  private fun collectExistingIds(course: Course): MutableSet<Int> {
    val ids = HashSet<Int>()
    course.visitItems { ids += it.id }
    return ids
  }

  companion object {

    fun getInstance(project: Project): StudyItemIdGenerator = project.service()

    private val Course.allItems: List<StudyItem>
      get() {
        val items = mutableListOf<StudyItem>()
        course.visitItems { items += it }
        return items
      }
  }
}
