package org.hyperskill.academy.learning.newproject.ui.coursePanel

import com.intellij.ide.impl.OpenProjectTask
import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.io.toNioPathOrNull
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.CourseMetadataProcessor
import org.hyperskill.academy.learning.newproject.CourseProjectState
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage

fun Course.openCourse(openCourseMetadata: Map<String, String>) {
  val coursesStorage = CoursesStorage.getInstance()
  val coursePath = coursesStorage.getCoursePath(this)?.toNioPathOrNull() ?: return
  val generator = configurator?.courseBuilder?.getCourseProjectGenerator(this) ?: return

  val pathToOpen = generator.setUpProjectLocation(coursePath)
  val beforeInitHandler = generator.beforeInitHandler(coursePath)
  val openProjectTask = OpenProjectTask {
    isNewProject = false
    projectToClose = null
    forceOpenInNewFrame = true
    projectName = course.name
    beforeInit = {
      beforeInitHandler.callback(it)
    }
  }
  val project = ProjectUtil.openProject(pathToOpen, openProjectTask)

  if (project != null) {
    CourseMetadataProcessor.applyProcessors(project, this, openCourseMetadata, CourseProjectState.OPENED_PROJECT)
    ProjectUtil.focusProjectWindow(project, true)
  }
}

fun showNoCourseDialog(coursePath: String, cancelButtonText: String): Int {
  return Messages.showDialog(
    null,
    EduCoreBundle.message("course.dialog.course.not.found.text", FileUtil.toSystemDependentName(coursePath)),
    EduCoreBundle.message("course.dialog.course.not.found.title"),
    arrayOf(Messages.getCancelButton(), cancelButtonText),
    Messages.OK,
    Messages.getErrorIcon()
  )
}