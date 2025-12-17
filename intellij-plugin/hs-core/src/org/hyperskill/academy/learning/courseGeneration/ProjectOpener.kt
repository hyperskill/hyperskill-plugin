package org.hyperskill.academy.learning.courseGeneration

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.authUtils.requestFocus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.CourseValidationResult
import org.hyperskill.academy.learning.courseFormat.ext.project
import org.hyperskill.academy.learning.stepik.builtInServer.EduBuiltInServerUtils

abstract class ProjectOpener {

  fun <T : OpenInIdeRequest> open(requestHandler: OpenInIdeRequestHandler<T>, request: T): Result<Boolean, CourseValidationResult> {
    runInEdt {
      // We might perform heavy operations (including network access)
      // So we want to request focus and show progress bar so as it won't seem that IDE doesn't respond
      requestFocus()
    }
    with(requestHandler) {
      val project = openInOpenedProject(request) ?: openInRecentProject(request)
      if (project != null) {
        invokeLater {
          afterProjectOpened(request, project)
        }
        return Ok(true)
      }

      return openInNewProject(request)
    }
  }

  private fun <T : OpenInIdeRequest> OpenInIdeRequestHandler<T>.openInOpenedProject(request: T): Project? =
    openInExistingProject(request, ::focusOpenProject)

  private fun <T : OpenInIdeRequest> OpenInIdeRequestHandler<T>.openInRecentProject(request: T): Project? =
    openInExistingProject(request, EduBuiltInServerUtils::openRecentProject)

  fun <T : OpenInIdeRequest> OpenInIdeRequestHandler<T>.openInNewProject(request: T): Result<Boolean, CourseValidationResult> {
    return computeUnderProgress(title = courseLoadingProcessTitle) { indicator ->
      getCourse(request, indicator)
    }.map { course ->
      runInEdt {
        requestFocus()
      }
      // JoinCourseDialog (newProject) must be created on EDT
      val openedRef = Ref<Boolean>()
      ApplicationManager.getApplication().invokeAndWait {
        openedRef.set(newProject(course))
      }
      val opened = openedRef.get()
      val project = course.project
      if (opened && project != null) {
        invokeLater {
          afterProjectOpened(request, project)
          // Ensure the newly created project window comes to the front
          ProjectUtil.focusProjectWindow(project, true)
        }
      }
      opened
    }
  }

  protected abstract fun newProject(course: Course): Boolean

  protected abstract fun focusOpenProject(coursePredicate: (Course) -> Boolean): Pair<Project, Course>?

  companion object {
    fun getInstance(): ProjectOpener = service()
  }
}