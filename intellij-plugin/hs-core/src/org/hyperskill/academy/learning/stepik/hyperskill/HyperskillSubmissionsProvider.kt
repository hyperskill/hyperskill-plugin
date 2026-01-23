package org.hyperskill.academy.learning.stepik.hyperskill

import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.authUtils.AuthorizationPlace
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.ext.allTasks
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.stepik.api.StepikBasedSubmission
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.hyperskill.academy.learning.submissions.provider.SubmissionsData
import org.hyperskill.academy.learning.submissions.provider.SubmissionsProvider

class HyperskillSubmissionsProvider : SubmissionsProvider {

  override fun loadAllSubmissions(course: Course): SubmissionsData {
    if (!areSubmissionsAvailable(course) || !isLoggedIn()) return emptyMap()
    return loadSubmissions(course.allTasks, course.id)
  }

  override fun loadSubmissions(tasks: List<Task>, courseId: Int): SubmissionsData {
    val stepIds = tasks.map { it.id }.toSet()
    val submissionsById = mutableMapOf<Int, MutableList<StepikBasedSubmission>>()
    val submissionsList = HyperskillConnector.getInstance().getSubmissions(stepIds)
    return submissionsList.groupByTo(submissionsById) { it.taskId }
  }

  override fun areSubmissionsAvailable(course: Course): Boolean {
    return course is HyperskillCourse
  }

  override fun getPlatformName(): String = EduNames.JBA

  override fun isLoggedIn(): Boolean = HyperskillSettings.INSTANCE.account != null

  override fun doAuthorize(vararg postLoginActions: Runnable) {
    HyperskillConnector.getInstance().doAuthorize(authorizationPlace = AuthorizationPlace.SUBMISSIONS_TAB)
  }
}