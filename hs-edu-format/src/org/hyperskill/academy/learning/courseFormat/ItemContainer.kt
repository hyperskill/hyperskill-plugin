package org.hyperskill.academy.learning.courseFormat

abstract class ItemContainer : StudyItem() {
  var items: List<StudyItem>
    get() = _items
    set(value) {
      _items = value.toMutableList()
    }

  private var _items = mutableListOf<StudyItem>()
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
  }

  fun addItem(index: Int, item: StudyItem) {
    _items.add(index, item)
    item.parent = this
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
