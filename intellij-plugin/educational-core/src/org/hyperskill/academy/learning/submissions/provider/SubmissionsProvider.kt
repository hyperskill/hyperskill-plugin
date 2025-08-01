package org.hyperskill.academy.learning.submissions.provider

import com.intellij.openapi.extensions.ExtensionPointName
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.submissions.Submission

/**
 * Base class for loading submissions, should be called only from SubmissionsManager.
 *
 * @see org.hyperskill.academy.learning.submissions.SubmissionsManager
 */
interface SubmissionsProvider {

  fun loadAllSubmissions(course: Course): SubmissionsData

  fun loadSubmissions(tasks: List<Task>, courseId: Int): SubmissionsData

  fun areSubmissionsAvailable(course: Course): Boolean

  fun isLoggedIn(): Boolean

  fun getPlatformName(): String

  fun doAuthorize(vararg postLoginActions: Runnable)

  companion object {
    private val EP_NAME = ExtensionPointName.create<SubmissionsProvider>("HyperskillEducational.submissionsProvider")

    fun getSubmissionsProviderForCourse(course: Course): SubmissionsProvider? {
      val submissionsProviders = EP_NAME.extensionList.filter { it.areSubmissionsAvailable(course) }
      if (submissionsProviders.isEmpty()) {
        return null
      }
      if (submissionsProviders.size > 1) {
        error("Several submissionsProviders available for ${course.name}: $submissionsProviders")
      }
      return submissionsProviders[0]
    }
  }
}

/**
 * Represents a collection of submissions grouped by their Task ID.
 */
typealias SubmissionsData = Map<Int, List<Submission>>