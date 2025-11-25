package org.hyperskill.academy.learning.projectView

import com.intellij.ide.projectView.ProjectView
import com.intellij.ide.ui.laf.darcula.ui.DarculaProgressBarUI
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.util.ColorProgressBar
import com.intellij.openapi.project.Project
import com.intellij.ui.Gray
import com.intellij.ui.JBColor
import org.hyperskill.academy.learning.StudyTaskManager
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.FrameworkLesson
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.ext.project
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.submissions.SubmissionsManager
import java.awt.Color
import javax.swing.JProgressBar

object ProgressUtil {

  fun countProgress(course: Course): CourseProgress {
    if (course is HyperskillCourse) {
      // we want empty progress in case project stages are not loaded
      // and only code challenges are present
      val projectLesson = course.getProjectLesson() ?: return CourseProgress(0, 0)
      return countProgress(projectLesson)
    }
    var taskNum = 0
    var taskSolved = 0
    course.visitLessons { lesson ->
      if (lesson is FrameworkLesson) {
        taskNum++
        if (lesson.taskList.all { it.status == CheckStatus.Solved }) {
          taskSolved++
        }
      }
      else {
        taskNum += lesson.taskList.size
        taskSolved += getSolvedTasks(lesson)
      }
    }
    return CourseProgress(taskSolved, taskNum)
  }

  fun countProgress(lesson: Lesson): CourseProgress {
    val taskNum = lesson.taskList.size
    val taskSolved = getSolvedTasks(lesson)
    return CourseProgress(taskSolved, taskNum)
  }

  private fun getSolvedTasks(lesson: Lesson): Int {
    return lesson.taskList
      .filter {
        val project = it.project ?: return@filter false
        it.status == CheckStatus.Solved || SubmissionsManager.getInstance(project).containsCorrectSubmission(it.id)
      }
      .count()
  }

  fun createProgressBar(): JProgressBar {
    val progressBar = JProgressBar()

    progressBar.setUI(object : DarculaProgressBarUI() {
      override fun getRemainderColor(): Color {
        return JBColor(Gray._220, Color(76, 77, 79))
      }
    })
    progressBar.foreground = ColorProgressBar.GREEN
    progressBar.isIndeterminate = false
    progressBar.putClientProperty("ProgressBar.flatEnds", true)
    return progressBar
  }

  fun updateCourseProgress(project: Project) {
    val course = StudyTaskManager.getInstance(project).course
    if (course == null) {
      LOG.error("course is null for project at ${project.basePath}")
      return
    }
    val progress = countProgress(course)
    val pane = ProjectView.getInstance(project).currentProjectViewPane
    if (pane is CourseViewPane && !ApplicationManager.getApplication().isUnitTestMode) {
      pane.updateCourseProgress(progress)
    }
    val location = project.basePath
    if (location != null) {
      CoursesStorage.getInstance().updateCourseProgress(course, location, progress.tasksSolved, progress.tasksTotalNum)
    }
  }

  private val LOG: Logger = Logger.getInstance(ProgressUtil::class.java)

  data class CourseProgress(val tasksSolved: Int, val tasksTotalNum: Int)
}
