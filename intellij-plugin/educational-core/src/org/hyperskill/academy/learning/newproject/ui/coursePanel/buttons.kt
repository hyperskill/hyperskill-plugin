package org.hyperskill.academy.learning.newproject.ui.coursePanel


import com.intellij.ide.plugins.newui.ColorButton
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.DialogWrapperDialog
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.io.FileUtil
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.CourseMode
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseGeneration.ProjectOpener
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.newproject.coursesStorage.CoursesStorage
import org.hyperskill.academy.learning.newproject.ui.JoinCourseDialog
import org.hyperskill.academy.learning.onError
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenProjectStageRequest
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import java.awt.Color
import java.awt.event.ActionListener

val SelectCourseBackgroundColor: Color
  get() = JBColor.namedColor(
    "SelectCourse.backgroundColor",
    JBColor(TaskToolWindowView.getTaskDescriptionBackgroundColor(), Color(0x313335))
  )
private val GreenColor: Color = JBColor(0x5D9B47, 0x2B7B50)
private val FillForegroundColor: Color = JBColor.namedColor("SelectCourse.Button.fillForeground", JBColor(Color.white, Color(0xBBBBBB)))
private val FillBackgroundColor: Color = JBColor.namedColor("SelectCourse.Button.fillBackground", GreenColor)
private val ForegroundColor: Color = JBColor.namedColor("SelectCourse.Button.foregroundColor", GreenColor)
private val FocusedBackground: Color = JBColor.namedColor("SelectCourse.Button.focusedBackground", JBColor(0xE1F6DA, 0xE1F6DA))
private val BorderColor: Color = JBColor.namedColor("SelectCourse.Button.border", GreenColor)

class OpenCourseButton(private val openCourseMetadata: () -> Map<String, String>) : CourseButtonBase() {

  override fun actionListener(course: Course): ActionListener = ActionListener {
    invokeLater {
      val coursesStorage = CoursesStorage.getInstance()
      val coursePath = coursesStorage.getCoursePath(course) ?: return@invokeLater
      if (!FileUtil.exists(coursePath)) {
        processMissingCourseOpening(course, coursePath)
        return@invokeLater
      }

      closeDialog()
      course.openCourse(openCourseMetadata())
    }
  }

  private fun processMissingCourseOpening(course: Course, coursePath: String) {
    val message = EduCoreBundle.message("course.dialog.course.not.found.reopen.button")

    if (showNoCourseDialog(coursePath, message) == Messages.NO) {
      CoursesStorage.getInstance().removeCourseByLocation(coursePath)
      when (course) {
        is HyperskillCourse -> {
          closeDialog()
          ProjectOpener.getInstance().apply {
            HyperskillOpenInIdeRequestHandler.openInNewProject(HyperskillOpenProjectStageRequest(course.id, null)).onError {
              Messages.showErrorDialog(it.message, EduCoreBundle.message("course.dialog.error.restart.jba"))
              logger<HyperskillOpenInIdeRequestHandler>().warn("Opening a new project resulted in an error: ${it.message}. The error was shown inside an error dialog.")
            }
          }
        }

        else -> {
          closeDialog()
          // if course is present both on stepik and marketplace we open marketplace-based one
          val courseToOpen = course
          JoinCourseDialog(courseToOpen).show()
        }
      }
    }
  }

  private fun closeDialog() {
    val dialog = UIUtil.getParentOfType(DialogWrapperDialog::class.java, this) ?: return
    dialog.dialogWrapper?.close(DialogWrapper.OK_EXIT_CODE)
  }

  override fun isVisible(course: Course): Boolean = CoursesStorage.getInstance().hasCourse(course)
}

/**
 * inspired by [com.intellij.ide.plugins.newui.InstallButton]
 */
class StartCourseButton(
  private val joinCourse: (Course, CourseMode) -> Unit,
  fill: Boolean = true
) : CourseButtonBase(fill) {

  override fun actionListener(course: Course) = ActionListener {
    joinCourse(course, CourseMode.STUDENT)
  }

  override fun isVisible(course: Course): Boolean {
    return !(CoursesStorage.getInstance().hasCourse(course))
  }
}

abstract class CourseButtonBase(fill: Boolean = false) : ColorButton() {
  private var listener: ActionListener? = null

  init {
    setTextColor(if (fill) FillForegroundColor else ForegroundColor)
    setBgColor(if (fill) FillBackgroundColor else SelectCourseBackgroundColor)
    setFocusedBgColor(FocusedBackground)
    setBorderColor(BorderColor)
    setFocusedBorderColor(BorderColor)
    setFocusedTextColor(ForegroundColor)
  }

  abstract fun isVisible(course: Course): Boolean

  protected abstract fun actionListener(course: Course): ActionListener

  open fun update(course: Course) {
    isVisible = isVisible(course)
    addListener(course)
  }

  private fun addListener(course: Course) {
    listener?.let { removeActionListener(listener) }
    isVisible = isVisible(course.course)
    if (isVisible) {
      listener = actionListener(course)
      addActionListener(listener)
    }
  }
}
