package org.hyperskill.academy.learning.update

import kotlinx.coroutines.runBlocking
import org.hyperskill.academy.learning.CourseBuilder
import org.hyperskill.academy.learning.actions.navigate.NavigationTestBase
import org.hyperskill.academy.learning.configurators.FakeGradleBasedLanguage
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.copy
import org.hyperskill.academy.learning.courseFormat.copyFileContents
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage

abstract class UpdateTestBase<T : Course> : NavigationTestBase() {
  protected lateinit var localCourse: T

  abstract fun getUpdater(localCourse: T): CourseUpdater<T>

  abstract fun initiateLocalCourse()

  override fun runInDispatchThread(): Boolean = false

  protected fun updateCourse(remoteCourse: T, isShouldBeUpdated: Boolean = true) {
    val updater = getUpdater(localCourse)
    val updates = runBlocking {
      updater.collect(remoteCourse)
    }
    assertEquals("Updates are" + (if (isShouldBeUpdated) " not" else "") + " available", isShouldBeUpdated, updates.isNotEmpty())
    val isUpdateSucceed = runBlocking {
      try {
        updater.update(remoteCourse)
        true
      }
      catch (e: Exception) {
        LOG.error(e)
        false
      }
    }
    if (isShouldBeUpdated) {
      assertTrue("Update failed", isUpdateSucceed)
    }
  }

  protected fun toRemoteCourse(changeCourse: T.() -> Unit): T =
    localCourse.copy().apply {
      additionalFiles = localCourse.additionalFiles
      copyFileContents(localCourse, this)
      changeCourse()
      init(false)
    }

  protected fun createBasicHyperskillCourse(buildCourse: (CourseBuilder.() -> Unit)? = null): HyperskillCourse {
    val title = "Hyperskill Project"
    val courseBuilder = buildCourse ?: {
      lesson("lesson1", id = 1) {
        eduTask("task1", stepId = 1) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
        eduTask("task2", stepId = 2) {
          taskFile("src/Task.kt")
          taskFile("src/Baz.kt")
          taskFile("test/Tests.kt")
        }
      }
      additionalFile("build.gradle", "apply plugin: \"java\"")
      additionalFile("settings.gradle")
    }
    val course = courseWithFiles(id = 1, name = title, language = FakeGradleBasedLanguage, courseProducer = ::HyperskillCourse) {
      courseBuilder()
    } as HyperskillCourse
    course.hyperskillProject = HyperskillProject().apply {
      this.title = title
      description = "Project Description"
    }
    course.stages = listOf(HyperskillStage(1, "", 1, true), HyperskillStage(2, "", 2))
    return course
  }

  protected fun checkIndices(items: List<StudyItem>) {
    val distinctIndices = items.map { it.index }.distinct().sorted()
    val expectedIndices = List(items.size) { it + 1 }
    assertEquals("Indices are incorrect", expectedIndices, distinctIndices)
  }
}