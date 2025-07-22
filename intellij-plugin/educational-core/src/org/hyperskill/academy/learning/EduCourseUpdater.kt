package org.hyperskill.academy.learning

import com.intellij.ide.projectView.ProjectView
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.stepik.StepikNames.STEPIK
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import java.io.IOException

abstract class EduCourseUpdater(val project: Project, val course: Course) {

  private val oldLessonDirectories = HashMap<Int, VirtualFile>()
  private val oldSectionDirectories = HashMap<Int, VirtualFile>()

  abstract fun courseFromServer(currentCourse: Course): Course?

  fun updateCourse() {
    checkIsBackgroundThread()
    oldLessonDirectories.clear()
    oldSectionDirectories.clear()

    val courseFromServer = courseFromServer(course)

    if (courseFromServer == null) {
      val platformName = STEPIK
      LOG.warn("Course ${course.id} not found on $platformName")
      return
    }

    // We are going to use the contents of course files, so we must init() the course.
    // Otherwise, we have only the course structure.
    courseFromServer.init(true)
    updateCourseWithRemote(courseFromServer)
  }

  fun updateCourseWithRemote(courseFromServer: Course) {
    doUpdate(courseFromServer)

    runInEdt {
      EduUtilsKt.synchronize()
      ProjectView.getInstance(project).refresh()
      course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
      YamlFormatSynchronizer.saveAll(project)
      project.messageBus.syncPublisher(CourseUpdateListener.COURSE_UPDATE).courseUpdated(project, course)
    }
  }

  protected abstract fun doUpdate(courseFromServer: Course)


  companion object {
    private val LOG = logger<EduCourseUpdater>()

    @Throws(IOException::class)
    fun createTaskDirectories(project: Project, lessonDir: VirtualFile, task: Task) {
      GeneratorUtils.createTask(project, task, lessonDir)
    }
  }
}