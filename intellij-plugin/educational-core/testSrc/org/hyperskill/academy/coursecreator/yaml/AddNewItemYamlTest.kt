package org.hyperskill.academy.coursecreator.yaml

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.ItemContainer
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.configFileName
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.hyperskill.academy.learning.yaml.YamlLoader
import org.hyperskill.academy.learning.yaml.YamlTestCase
import org.junit.Test

class AddNewItemYamlTest : YamlTestCase() {

  override fun setUp() {
    super.setUp()
    // as we don't create config file in CCProjectComponent, we have to create them manually
    createConfigFiles(project)
  }

  override fun createCourse() {
    courseWithFiles(courseMode = CourseMode.STUDENT, description = "test") {
      lesson("lesson1") {
        eduTask("task1") {
          taskFile("test1.txt")
          taskFile("test2.txt")
        }
        eduTask("task2")
      }
      lesson("lesson2") {
        eduTask("task1")
      }
      section("section1") {
        lesson("lesson1") {
          eduTask("task1")
        }
      }
      section("section2") {
        lesson("lesson1") {
          eduTask("task1")
        }
      }
    }
  }

  @Test
  fun `test new task file added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    val task = course.lessons.first().taskList.first()
    task.removeTaskFile("test2.txt")

    val configFile = task.getDir(project.courseDir)!!.findChild(task.configFileName)!!
    loadAndDispatchEvents(configFile)

    assertEquals(2, task.taskFiles.size)
  }

  @Test
  fun `test unexpected task file isn't added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    val task = course.lessons.first().taskList.first()
    task.removeTaskFile("test2.txt")

    saveItemAndDispatchEvents(task)

    val configFile = task.getDir(project.courseDir)!!.findChild(task.configFileName)!!
    loadAndDispatchEvents(configFile)

    assertEquals(1, task.taskFiles.size)
  }

  @Test
  fun `test new task added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doChildrenAddedTest(course.lessons.first())
  }

  @Test
  fun `test missing task added to parent`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doToParentAddedTest(course.lessons.first())
  }

  @Test
  fun `test new unexpected task isn't added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doNotAddedTest(course.lessons.first())
  }

  @Test
  fun `test new lesson added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doChildrenAddedTest(course)
  }

  @Test
  fun `test missing lesson added to parent`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doToParentAddedTest(course.getLesson("lesson1") ?: error("no lesson1 found"))
  }

  @Test
  fun `test new lesson content added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    val section = course.sections.first()
    doChildrenAddedTest(section)
    val lesson = section.items.last() as Lesson
    assertEquals(1, lesson.taskList.size)
  }

  @Test
  fun `test new unexpected lesson isn't added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doNotAddedTest(course)
  }

  @Test
  fun `test new section added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doChildrenAddedTest(course)
  }

  @Test
  fun `test missing section added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doToParentAddedTest(course)
  }

  @Test
  fun `test new section content added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doChildrenAddedTest(course)
  }

  @Test
  fun `test new unexpected section isn't added`() {
    val course = StudyTaskManager.getInstance(project).course!!
    doNotAddedTest(course)
  }

  private fun ItemContainer.removeLastItem() {
    removeItem(items.last())
  }

  /**
   * Checks the case when user added child in parent config
   */
  private fun doChildrenAddedTest(itemContainer: ItemContainer) {
    val expectedSize = itemContainer.items.size
    itemContainer.removeLastItem()

    val configFile = itemContainer.getDir(project.courseDir)!!.findChild(itemContainer.configFileName)!!
    loadAndDispatchEvents(configFile)

    assertEquals(expectedSize, itemContainer.items.size)
  }

  /**
   * Checks the case when user fixed broken config and so child can be added to its parent
   */
  private fun doToParentAddedTest(itemContainer: ItemContainer) {
    val expectedSize = itemContainer.items.size
    val lastChild = itemContainer.items.last()
    val lastChildConfig = lastChild.getDir(project.courseDir)?.findChild(lastChild.configFileName)!!
    itemContainer.removeLastItem()

    loadAndDispatchEvents(lastChildConfig)
    assertEquals(expectedSize, itemContainer.items.size)
  }

  private fun doNotAddedTest(itemContainer: ItemContainer) {
    val expectedSize = itemContainer.items.size - 1
    val lastChild = itemContainer.items.last()
    val lastChildConfig = lastChild.getDir(project.courseDir)?.findChild(lastChild.configFileName)!!
    itemContainer.removeLastItem()

    saveItemAndDispatchEvents(itemContainer)

    loadAndDispatchEvents(lastChildConfig)

    assertEquals(expectedSize, itemContainer.items.size)
  }

  private fun loadAndDispatchEvents(lastChildConfig: VirtualFile) {
    YamlLoader.loadItem(project, lastChildConfig, true)
    UIUtil.dispatchAllInvocationEvents()
  }

  private fun saveItemAndDispatchEvents(studyItem: StudyItem) {
    YamlFormatSynchronizer.saveItem(studyItem)
    UIUtil.dispatchAllInvocationEvents()
  }
}