package org.hyperskill.academy.learning.format

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.xmlb.XmlSerializer
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.newproject.coursesStorage.UserCoursesState
import org.hyperskill.academy.learning.stepik.hyperskill.PlainTextHyperskillConfigurator
import org.hyperskill.academy.learning.stepik.hyperskill.hyperskillCourse
import org.jdom.Element
import org.junit.Test
import java.nio.file.Paths

open class CoursesInfosStorageTestBase : EduTestCase() {

  @Test
  fun `test correct configurator found for courses in storage`() {
    val coursesStorage = CoursesStorage.getInstance()

    val hyperskillCourse = hyperskillCourse(language = PlainTextLanguage.INSTANCE) {}
    for ((course, configuratorClass) in listOf(
      hyperskillCourse to PlainTextHyperskillConfigurator::class.java
    )) {
      coursesStorage.addCourse(course, "location", 0, 0)
      assertInstanceOf(coursesStorage.getCourseMetaInfo(course)!!.toCourse().configurator, configuratorClass)
    }
  }

  @Test
  fun testCourseIdRespected() {
    val coursesStorage = CoursesStorage.getInstance()
    val courseWithDefaultId = course {}
    coursesStorage.addCourse(courseWithDefaultId, "", 0, 0)
    val studentCourse = course {}.apply { id = 1234 }
    assertFalse(coursesStorage.hasCourse(studentCourse))
  }

  @Test
  fun testLanguageRespected() {
    val coursesStorage = CoursesStorage.getInstance()
    val courseWithDefaultId = course {}
    coursesStorage.addCourse(courseWithDefaultId, "", 0, 0)
    val courseWithLanguage = course {}.apply { languageId = EduFormatNames.PYTHON }
    assertFalse(coursesStorage.hasCourse(courseWithLanguage))
  }

  @Test
  fun testDeserializeFirstVersionCoursesStorage() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("Introduction to Python", course.name)
    assertEquals(238, course.id)
    assertEquals("\$USER_HOME$/IdeaProjects/Introduction to Python", course.location)
    assertEquals("Introduction course to Python.", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduFormatNames.PYTHON, course.languageId)
    assertEquals("2.7", course.languageVersion)
  }

  @Test
  fun testDeserializeCourseWithDefaultParameters() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals("kotlin", course.languageId)
  }

  @Test
  fun testDeserializeOldLanguageVersion() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduFormatNames.PYTHON, course.languageId)
    assertEquals("3.7", course.languageVersion)
  }

  // Such case shouldn't happen, but this test is useful for migration testing
  @Test
  fun testDeserializeNewLanguageVersion() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduFormatNames.PYTHON, course.languageId)
    assertEquals("3.7", course.languageVersion)
  }

  @Test
  fun testDeserializeNewLanguageVersionAndLanguageId() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduFormatNames.PYTHON, course.languageId)
    assertEquals("3.7", course.languageVersion)
  }

  // Such case shouldn't happen, but this test is useful for migration testing
  @Test
  fun testDeserializeNewAndOldLanguageVersion() {
    val deserialized = deserializeState()
    assertEquals(1, deserialized.courses.size)
    val course = deserialized.courses.first()
    assertEquals("AtomicKotlin", course.name)
    assertEquals(20403, course.id)
    assertEquals("\$USER_HOME$/IdeaProjects/AtomicKotlin", course.location)
    assertEquals("The examples and exercises accompanying the AtomicKotlin book", course.description)
    assertEquals("PyCharm", course.type)
    assertEquals(EduFormatNames.PYTHON, course.languageId)
    assertEquals("3.7", course.languageVersion)
  }


  @Test
  fun testEmptyCoursesGroup() {
    val coursesStorage = getCoursesStorage()
    assertEmpty(coursesStorage.coursesInGroups())
  }

  @Test
  fun testInProgressCoursesGroup() {
    val coursesStorage = getCoursesStorage()
    val course = course {}
    val location = createTempCourseLocation()
    coursesStorage.addCourse(course, location, 1, 10)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups.first().name)
  }

  @Test
  fun testCompletedCoursesGroup() {
    val coursesStorage = getCoursesStorage()
    val course = course {}
    val location = createTempCourseLocation()
    coursesStorage.addCourse(course, location, 10, 10)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.completed"), coursesInGroups.first().name)
  }

  @Test
  fun testUntouchedCourse() {
    val coursesStorage = getCoursesStorage()
    val course = course {}
    val location = createTempCourseLocation()
    coursesStorage.addCourse(course, location, 0, 0)
    val coursesInGroups = coursesStorage.coursesInGroups()
    assertSize(1, coursesInGroups)
    assertEquals(EduCoreBundle.message("course.dialog.in.progress"), coursesInGroups.first().name)
  }

  /**
   * Creates a temporary directory for the course location that is visible to VFS.
   * The VFS check in [JBACourseFromStorage.isLocationValid] requires the directory to be indexed.
   */
  private fun createTempCourseLocation(): String {
    val tempDir = kotlin.io.path.createTempDirectory("course-test").toFile()
    tempDir.deleteOnExit()
    // Refresh VFS to make the directory visible
    com.intellij.openapi.vfs.LocalFileSystem.getInstance().refreshAndFindFileByIoFile(tempDir)
    return tempDir.absolutePath
  }

  private fun getCoursesStorage(): CoursesStorage {
    val coursesStorage = CoursesStorage.getInstance()
    coursesStorage.state.courses.clear()
    return coursesStorage
  }

  protected fun deserializeState(): UserCoursesState {
    val element = loadFromFile()
    return XmlSerializer.deserialize(element.children.first(), UserCoursesState::class.java)
  }

  private fun loadFromFile(): Element {
    val name = getTestName(true)
    val loaded = Paths.get(testDataPath).resolve("$name.xml")
    return JDOMUtil.load(loaded)
  }

  override fun getTestDataPath() = "testData/coursesStorage"
}