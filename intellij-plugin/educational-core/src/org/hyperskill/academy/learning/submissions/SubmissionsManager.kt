package org.hyperskill.academy.learning.submissions

import com.intellij.execution.process.ProcessIOExecutorService
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import org.hyperskill.academy.learning.EduTestAware
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.CORRECT
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.createTopic
import org.hyperskill.academy.learning.submissions.provider.SubmissionsProvider
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

/**
 * Stores and returns submissions for courses with submissions support if they are already loaded or delegates loading
 * to SubmissionsProvider.
 *
 * @see org.hyperskill.academy.learning.submissions.provider.SubmissionsProvider
 */
@Service(Service.Level.PROJECT)
class SubmissionsManager(private val project: Project) : EduTestAware {

  private val submissions = ConcurrentHashMap<Int, List<Submission>>()

  var course: Course? = project.course
    @TestOnly set

  fun getSubmissionsFromMemory(taskIds: Set<Int>): List<Submission> {
    return taskIds.mapNotNull { submissions[it] }.flatten().sortedByDescending { it.time }
  }

  fun getOrLoadSubmissions(tasks: List<Task>): List<Submission> {
    val taskIds = tasks.map { it.id }.toSet()
    val submissionsFromMemory = getSubmissionsFromMemory(taskIds)
    return submissionsFromMemory.ifEmpty {
      val course = course ?: error("Nullable Course")
      val submissionsProvider =
        SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: error("SubmissionProvider for course ${course.id} not available")
      val submissionsById = submissionsProvider.loadSubmissions(tasks, course.id)
      submissions.putAll(submissionsById)
      notifySubmissionsChanged()
      submissionsById.values.flatten()
    }
  }


  fun getSubmissionWithSolutionText(task: Task, submissionId: Int): Submission? {
    val submission = getOrLoadSubmissions(task).find { it.id == submissionId } ?: return null

    return submission
  }

  private fun getOrLoadSubmissions(task: Task): List<Submission> {
    val course = course ?: return emptyList()
    val submissionsProvider = course.getSubmissionsProvider() ?: return emptyList()
    val submissionsList = submissions[task.id]
    return if (submissionsList != null) {
      submissionsList
    }
    else {
      val loadedSubmissions = submissionsProvider.loadSubmissions(listOf(task), course.id)
      submissions.putAll(loadedSubmissions)
      notifySubmissionsChanged()
      return loadedSubmissions[task.id] ?: emptyList()
    }
  }

  fun addToSubmissions(taskId: Int, submission: Submission) {
    val submissionsList = submissions.getOrPut(taskId) { listOf(submission) }.toMutableList()
    if (!submissionsList.contains(submission)) {
      submissionsList.add(submission)
      submissionsList.sortByDescending { it.time }
      submissions[taskId] = submissionsList
      //potential race when loading submissions and checking task at one time
    }
    notifySubmissionsChanged()
  }

  private fun notifySubmissionsChanged() {
    project.messageBus.syncPublisher(TOPIC).submissionsChanged()
  }

  fun containsCorrectSubmission(stepId: Int): Boolean {
    return getSubmissionsFromMemory(setOf(stepId)).any { it.status == CORRECT }
  }

  fun addToSubmissionsWithStatus(taskId: Int, checkStatus: CheckStatus, submission: Submission?) {
    if (submission == null || checkStatus == CheckStatus.Unchecked) return
    submission.status = checkStatus.rawStatus
    addToSubmissions(taskId, submission)
  }

  fun submissionsSupported(): Boolean {
    val course = course
    if (course == null) return false
    val submissionsProvider = SubmissionsProvider.getSubmissionsProviderForCourse(course) ?: return false
    return submissionsProvider.areSubmissionsAvailable(course)
  }

  fun prepareSubmissionsContentWhenLoggedIn(loadSolutions: () -> Unit = {}) {
    val course = course
    val submissionsProvider = course?.getSubmissionsProvider() ?: return

    CompletableFuture.runAsync({
      if (!isLoggedIn()) return@runAsync
      val taskToolWindowView = TaskToolWindowView.getInstance(project)
      val platformName = getPlatformName()

      taskToolWindowView.showLoadingSubmissionsPanel(platformName)
      loadSubmissionsContent(course, submissionsProvider, loadSolutions)

      notifySubmissionsChanged()
    }, ProcessIOExecutorService.INSTANCE)
  }

  fun isLoggedIn(): Boolean = course?.getSubmissionsProvider()?.isLoggedIn() ?: false

  private fun getPlatformName(): String = course?.getSubmissionsProvider()?.getPlatformName() ?: error("Failed to get platform Name")

  fun doAuthorize() = course?.getSubmissionsProvider()?.doAuthorize({ prepareSubmissionsContentWhenLoggedIn() })

  private fun loadSubmissionsContent(course: Course, submissionsProvider: SubmissionsProvider, loadSolutions: () -> Unit) {
    submissions.putAll(submissionsProvider.loadAllSubmissions(course))
    loadSolutions()
  }

  private fun Course.getSubmissionsProvider(): SubmissionsProvider? {
    return SubmissionsProvider.getSubmissionsProviderForCourse(this)
  }

  fun getLastSubmission(): Submission? {
    return submissions.values.flatten().sortedByDescending { it.time }.firstOrNull()
  }

  @TestOnly
  override fun cleanUpState() {
    submissions.clear()
  }

  companion object {

    @Topic.ProjectLevel
    val TOPIC: Topic<SubmissionsListener> = createTopic("Hyperskill.submissions")
    
    fun getInstance(project: Project): SubmissionsManager {
      return project.service()
    }
  }
}

fun interface SubmissionsListener {
  fun submissionsChanged()
}

fun interface SharedSolutionsListener {
  fun sharedSolutionsUnchanged()
}