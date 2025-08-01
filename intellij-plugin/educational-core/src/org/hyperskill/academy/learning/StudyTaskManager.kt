package org.hyperskill.academy.learning

import com.intellij.ide.lightEdit.LightEdit
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.yaml.YamlDeepLoader.loadCourse
import org.hyperskill.academy.learning.yaml.YamlFormatSettings.isEduYamlProject
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer.startSynchronization
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Implementation of class which contains all the information about study in context of current project
 */
@Service(Service.Level.PROJECT)
class StudyTaskManager(private val project: Project) : DumbAware, Disposable, EduTestAware {
  @Volatile
  private var courseLoadedWithError = false

  private val courseLoadingLock = ReentrantLock()

  @Volatile
  private var _course: Course? = null

  var course: Course?
    get() = _course
    set(course) {
      _course = course
      course?.fireCourseSetEvent()
    }

  private fun Course.fireCourseSetEvent() =
    project.messageBus.syncPublisher(COURSE_SET).courseSet(this)

  private fun needToLoadCourse(project: Project): Boolean =
    !project.isDefault
    && !LightEdit.owns(project)
    && course == null
    && !courseLoadedWithError
    && project.isEduYamlProject()

  private fun initializeCourse() {
    if (!needToLoadCourse(project)) return

    var loadedCourse: Course? = null

    courseLoadingLock.withLock {
      if (!needToLoadCourse(project)) return

      loadedCourse = runReadAction {
        try {
          loadCourse(project)
        }
        catch (th: Throwable) {
          LOG.error("Error while loading course", th)
          null
        }
      }
      courseLoadedWithError = loadedCourse == null
      if (loadedCourse != null) {
        logger<StudyTaskManager>().info("Loaded course corresponding to the project: ${loadedCourse.name}")
        _course = loadedCourse
      }
      else {
        logger<StudyTaskManager>().info("Course corresponding to the project loaded with errors")
      }
    }

    loadedCourse?.fireCourseSetEvent()
    startSynchronization(project)
  }

  override fun dispose() {}

  @TestOnly
  override fun cleanUpState() {
    course = null
  }

  companion object {
    val COURSE_SET = Topic.create("Hyperskill.courseSet", CourseSetListener::class.java)
    val LOG = logger<StudyTaskManager>()

    fun getInstance(project: Project): StudyTaskManager = project.service<StudyTaskManager>().apply {
      initializeCourse()
    }
  }
}
