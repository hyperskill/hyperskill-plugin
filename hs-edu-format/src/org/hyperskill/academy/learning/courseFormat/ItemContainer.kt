package org.hyperskill.academy.learning.courseFormat

abstract class ItemContainer : StudyItem() {
  var items: List<StudyItem>
    get() = _items
    set(value) {
      _items = value.toMutableList()
    }

  private var _items = mutableListOf<StudyItem>()

  /**
   * Entries of this container's `content:` list that could not be resolved during the last load: the child directory or
   * its config file was missing, or the config failed to deserialize. They are not part of [items], but [contentNames]
   * still writes them back, so saving the container neither drops them from the config file on disk nor has to be
   * skipped -- a container that refused to save would silently lose every child added to it later.
   *
   * Recomputed on every load, so a later complete load clears it again.
   */
  @Transient
  var unresolvedContent: List<UnresolvedContentEntry> = emptyList()

  /**
   * The `content:` list of this container's config file: the names of [items] in their order, with each
   * [unresolvedContent] entry kept right after the entry it followed on disk.
   */
  val contentNames: List<String>
    get() {
      val names = items.map { it.name }
      if (unresolvedContent.isEmpty()) return names

      val present = names.toSet()
      val (anchored, orphaned) = unresolvedContent
        .filter { it.name !in present }
        .partition { it.previousName == null || it.previousName in present }
      val namesByAnchor = anchored.groupBy({ it.previousName }, { it.name })
      return buildList {
        addAll(namesByAnchor[null].orEmpty())
        for (name in names) {
          add(name)
          addAll(namesByAnchor[name].orEmpty())
        }
        // The entry they followed is gone from the container, so there is no position left to keep.
        addAll(orphaned.map { it.name })
      }
    }

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    parent = parentItem
    for ((i, item) in items.withIndex()) {
      item.index = i + 1
      item.init(this, isRestarted)
    }
  }

  fun getItem(name: String): StudyItem? {
    return items.firstOrNull { it.name == name }
  }

  /**
   * Adding an item also makes this container its [StudyItem.parent].
   *
   * Otherwise the item would stay in the container while [StudyItem.parent] throws, and every consumer walking up
   * the tree (course view presentation, YAML reload, task description) breaks on an item that looks perfectly fine
   * in [items]. [init] still (re)initializes a whole subtree, this only keeps a single insertion consistent.
   */
  fun addItem(item: StudyItem) {
    _items.add(item)
    item.parent = this
    forgetUnresolved(item.name)
  }

  fun addItem(index: Int, item: StudyItem) {
    _items.add(index, item)
    item.parent = this
    forgetUnresolved(item.name)
  }

  // From now on the item owns this name, so removing it later must not bring the unresolved entry back.
  private fun forgetUnresolved(name: String) {
    if (unresolvedContent.any { it.name == name }) {
      unresolvedContent = unresolvedContent.filter { it.name != name }
    }
  }

  fun replaceItem(existingItem: StudyItem, newItem: StudyItem) {
    val index = _items.indexOf(existingItem)
    if (index < 0) return
    _items[index] = newItem
    newItem.parent = this
  }

  fun removeItem(item: StudyItem) {
    _items.remove(item)
  }

  open fun sortItems() {
    _items.sortBy { it.index }
  }
}

/**
 * A `content:` entry named [name] that could not be resolved during load. [previousName] is the closest resolved entry
 * before it (`null` if there is none), so [ItemContainer.contentNames] can write it back at the same place.
 */
data class UnresolvedContentEntry(val name: String, val previousName: String?)
