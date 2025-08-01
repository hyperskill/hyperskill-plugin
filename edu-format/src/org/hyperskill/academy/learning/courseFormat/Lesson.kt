package org.hyperskill.academy.learning.courseFormat

import org.hyperskill.academy.learning.courseFormat.EduFormatNames.LESSON
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeLesson

/**
 * To introduce new lesson type it's required to:
 * - Extend Lesson class
 * - Handle yaml deserialization [org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeLesson]
 */
open class Lesson : ItemContainer() {

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    require(parentItem is LessonContainer) { "Parent for lesson $name should be either course or section" }
    super.init(parentItem, isRestarted)
  }

  /**
   * Returns tasks copy. Dedicated methods should be used to modify list of lesson items ([addTask], [removeTask])
   */
  val taskList: List<Task>
    get() = items.filterIsInstance<Task>()

  override val course: Course
    get() = parent.course
  val section: Section?
    get() = parent as? Section

  override val itemType: String = LESSON

  fun addTask(task: Task) {
    addItem(task)
  }

  fun addTask(index: Int, task: Task) {
    addItem(index, task)
  }

  fun removeTask(task: Task) {
    removeItem(task)
  }

  fun getTask(name: String): Task? {
    return getItem(name) as? Task
  }

  fun getTask(id: Int): Task? {
    return taskList.firstOrNull { it.id == id }
  }

  val container: LessonContainer
    get() = section ?: course

  fun visitTasks(visit: (Task) -> Unit) {
    taskList.forEach(visit)
  }
}
