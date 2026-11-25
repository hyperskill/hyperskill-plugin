package org.hyperskill.academy.learning.yaml

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.coursecreator.yaml.createConfigFiles
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseDir
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.ext.getDir
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.configFileName
import org.junit.Test

/**
 * A child listed in a container's `content:` that cannot be resolved on load (its directory was deleted, its config
 * is broken) must neither be dropped from the config file nor stop the container from being saved.
 */
class UnresolvedContentYamlTest : YamlTestCase() {

  @Test
  fun `test lesson added to a partially loaded section survives reload`() {
    createSectionWithLessons("lesson1", "lesson2", "lesson3")
    deleteCourseDir("Topics/lesson2")

    val section = reloadSection()
    assertEquals(listOf("lesson1", "lesson3"), section.items.map { it.name })
    assertEquals(listOf(UnresolvedContentEntry("lesson2", "lesson1")), section.unresolvedContent)

    addLessonAndSave(section, "lesson4")

    assertEquals(listOf("lesson1", "lesson2", "lesson3", "lesson4"), contentOnDisk(section))
    assertEquals(listOf("lesson1", "lesson3", "lesson4"), reloadSection().items.map { it.name })
  }

  @Test
  fun `test complete load clears unresolved content`() {
    createSectionWithLessons("lesson1", "lesson2")
    val lessonConfig = project.courseDir.findFileByRelativePath("Topics/lesson2/${YamlConfigSettings.LESSON_CONFIG}")!!
    val lessonConfigText = VfsUtil.loadText(lessonConfig)
    runWriteAction { lessonConfig.delete(this) }

    assertEquals(listOf("lesson2"), reloadSection().unresolvedContent.map { it.name })

    runWriteAction {
      val lessonDir = project.courseDir.findFileByRelativePath("Topics/lesson2")!!
      VfsUtil.saveText(lessonDir.createChildData(this, YamlConfigSettings.LESSON_CONFIG), lessonConfigText)
    }

    val section = reloadSection()
    assertEquals(listOf("lesson1", "lesson2"), section.items.map { it.name })
    assertEmpty(section.unresolvedContent)
  }

  @Test
  fun `test re-created child replaces its unresolved entry`() {
    createSectionWithLessons("lesson1", "lesson2", "lesson3")
    deleteCourseDir("Topics/lesson2")
    val section = reloadSection()

    addLessonAndSave(section, "lesson2")

    assertEmpty(section.unresolvedContent)
    assertEquals(listOf("lesson1", "lesson3", "lesson2"), contentOnDisk(section))
  }

  @Test
  fun `test unresolved entries are written back where they were`() {
    val section = Section()
    section.items = listOf("a", "c").map { Lesson().apply { name = it } }
    section.unresolvedContent = listOf(
      UnresolvedContentEntry("first", null),
      UnresolvedContentEntry("b1", "a"),
      UnresolvedContentEntry("b2", "a"),
      UnresolvedContentEntry("orphan", "removed"),
      UnresolvedContentEntry("c", "a"),
    )

    // `c` is present again, and `orphan` lost the entry it followed, so it goes last
    assertEquals(listOf("first", "a", "b1", "b2", "c", "orphan"), section.contentNames)
  }

  private fun createSectionWithLessons(vararg lessonNames: String) {
    courseWithFiles {
      section("Topics") {
        for (lessonName in lessonNames) {
          lesson(lessonName) {
            eduTask("task1") {
              taskFile("Task.txt")
            }
          }
        }
      }
    }
    createConfigFiles(project)
  }

  private fun deleteCourseDir(path: String) {
    runWriteAction { project.courseDir.findFileByRelativePath(path)!!.delete(this) }
  }

  private fun reloadSection(): Section {
    val course = YamlDeepLoader.loadCourse(project)!!
    StudyTaskManager.getInstance(project).course = course
    return course.getItem("Topics") as Section
  }

  // The same steps as `HyperskillOpenInIdeRequestHandler` takes to add a topic opened from the browser
  private fun addLessonAndSave(section: Section, lessonName: String) {
    val lesson = Lesson()
    lesson.name = lessonName
    lesson.addTask(EduTask("task1"))
    section.addLesson(lesson)
    section.init(section.course, false)

    GeneratorUtils.createLesson(project, lesson, section.getDir(project.courseDir)!!)
    lesson.taskList.forEach { YamlFormatSynchronizer.saveItem(it) }
    YamlFormatSynchronizer.saveItem(lesson)
    YamlFormatSynchronizer.saveItem(section)
    UIUtil.dispatchAllInvocationEvents()
  }

  private fun contentOnDisk(item: StudyItem): List<String> {
    val configFile = item.getConfigDir(project)!!.findChild(item.configFileName)!!
    return YamlMapper.basicMapper().readTree(VfsUtil.loadText(configFile))["content"].map { it.asText() }
  }
}
