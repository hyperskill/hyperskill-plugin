package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType.ERROR
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.ui.HyperlinkAdapter
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.configuration.EduConfiguratorManager
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTopic
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.courseGeneration.ProjectOpener
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.notification.EduNotificationManager
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillAccount
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillStepSource
import org.hyperskill.academy.learning.stepik.hyperskill.api.WithPaginationMetaData
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenStepWithProjectRequest
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import javax.swing.event.HyperlinkEvent

private val LOG: Logger = Logger.getInstance("HyperskillUtils")

fun openSelectedStage(course: Course, project: Project) {
  if (course !is HyperskillCourse) {
    return
  }
  val stageId = computeSelectedStage(course)
  if (stageId != null) {
    val index = course.stages.indexOfFirst { stage -> stage.id == stageId }
    if (course.lessons.isNotEmpty()) {
      val lesson = course.lessons[0]
      val taskList = lesson.taskList
      if (taskList.size > index) {
        val fromTask = if (lesson is FrameworkLesson) lesson.currentTask() else taskList[0]
        NavigationUtils.navigateToTask(project, taskList[index], fromTask, false)
      }
    }
  }
}

private fun computeSelectedStage(course: HyperskillCourse): Int? {
  val stageId = course.selectedStage
  if (stageId != null) {
    course.selectedStage = null  // we may want to select something other in the same project
    return stageId
  }
  // do not switch selected stage if a user opened only a single problem
  val problemId = course.selectedProblem
  if (problemId != null) {
    course.selectedProblem = null // we may want to select something other in the same project
    return null
  }
  val projectLesson = course.getProjectLesson() ?: return null
  val firstUnsolvedTask = projectLesson.taskList.indexOfFirst { task -> task.status != CheckStatus.Solved }
  if (firstUnsolvedTask == -1 && projectLesson.taskList.isEmpty()) return null
  return course.stages[if (firstUnsolvedTask != -1) firstUnsolvedTask else projectLesson.taskList.size - 1].id
}

fun markStageAsCompleted(task: Task) {
  val course = task.course as HyperskillCourse
  val stage = course.stages.getOrNull(task.index - 1) ?: error("No stage for stage ${task.name} in course ${course.name}")
  if (stage.isCompleted) return
  stage.isCompleted = true
  YamlFormatSynchronizer.saveRemoteInfo(course)
}

fun hyperskillTaskLink(task: Task): String {
  val course = task.course as? HyperskillCourse ?: error("Course is not a Hyperskill course")
  return if (course.isTaskInProject(task)) stageLink(task) else stepLink(task.id)
}

fun stageLink(task: Task): String {
  val course = task.course as? HyperskillCourse ?: error("Course is not a Hyperskill course")
  val projectId = course.hyperskillProject?.id ?: error("Course doesn't have Hyperskill project")
  val stageId = course.stages[task.index - 1].id
  return stageLink(projectId, stageId)
}

fun stageLink(projectId: Int, stageId: Int) = "$HYPERSKILL_PROJECTS_URL/$projectId/stages/$stageId/implement"

fun stepLink(stepId: Int) = "${HYPERSKILL_URL}/learn/step/$stepId"

/**
 * If lesson is not complete when next step appear
 */
fun topicCompletedLink(topicId: Int) = "${HYPERSKILL_URL}/learn/topic/${topicId}"

fun isHyperskillSupportAvailable(): Boolean = EduConfiguratorManager.allExtensions().any { it.courseType == HYPERSKILL }

fun getSelectedProjectIdUnderProgress(): Int? {
  return computeUnderProgress(null, EduCoreBundle.message("hyperskill.synchronizing.account"), false) {
    val currentUser = HyperskillConnector.getInstance().getCurrentUserInfo() ?: return@computeUnderProgress null
    currentUser.hyperskillProjectId
  }
}

object HyperskillLoginListener : HyperlinkAdapter() {
  override fun hyperlinkActivated(e: HyperlinkEvent) {
    doLogin()
  }

  fun doLogin() {
    HyperskillConnector.getInstance().doAuthorize(Runnable {
      val fullName = HyperskillSettings.INSTANCE.account?.userInfo?.getFullName() ?: return@Runnable
      EduNotificationManager.showInfoNotification(
        title = EduCoreBundle.message("login.successful"),
        content = EduCoreBundle.message("logged.in.as", fullName)
      )
    })
  }
}

val HyperskillProject.eduEnvironment: String?
  get() {
    val hyperskillEnvironment = environment
    if (hyperskillEnvironment.isNullOrEmpty()) {
      return DEFAULT_ENVIRONMENT
    }
    return HYPERSKILL_ENVIRONMENTS[hyperskillEnvironment]
  }

val HyperskillAccount.profileUrl: String get() = "$HYPERSKILL_PROFILE_PATH${userInfo.id}"

fun Task.getRelatedTheoryTask(): TheoryTask? {
  if (course !is HyperskillCourse) return null
  if (this is TheoryTask) {
    LOG.warn("Function is called for Theory task itself")
    return null
  }
  return lesson.taskList.find { it is TheoryTask } as? TheoryTask
}

fun notifyJBAUnauthorized(project: Project, specificMessage: String) {
  EduNotificationManager
    .create(ERROR, specificMessage, EduCoreBundle.message("notification.hyperskill.no.next.activity.login.content"))
    .addAction(NotificationAction.createSimpleExpiring(EduCoreBundle.message("notification.hyperskill.no.next.activity.login.action")) {
      HyperskillLoginListener.doLogin()
    })
    .notify(project)
}

fun openNextActivity(project: Project, task: Task) {
  if (HyperskillSettings.INSTANCE.account == null) {
    notifyJBAUnauthorized(project, EduCoreBundle.message("notification.hyperskill.no.next.activity.title"))
    return
  }

  val nextActivityInfo = computeUnderProgress(project, EduCoreBundle.message("hyperskill.next.activity"), true) {
    getNextStep(task.id)
  }

  openStep(project, task, nextActivityInfo)
}

fun openTopic(project: Project, topic: HyperskillTopic) {
  if (HyperskillSettings.INSTANCE.account == null) {
    notifyJBAUnauthorized(project, EduCoreBundle.message("notification.hyperskill.no.next.activity.title"))
    return
  }

  val nextActivityInfo = computeUnderProgress(project, EduCoreBundle.message("hyperskill.topics.fetch"), true) {
    getNextStepInTopic(topic.id, null)
  }

  openStep(project, null, nextActivityInfo)
}

private fun openStep(project: Project, task: Task?, nextActivityInfo: NextActivityInfo) {
  when (nextActivityInfo) {
    is NextActivityInfo.TopicCompleted -> {
      val nextTask = task?.lesson?.let { NavigationUtils.nextLesson(it)?.taskList?.firstOrNull() }
      if (nextTask != null) {
        NavigationUtils.navigateToTask(project, nextTask)
      }
      else {
        EduBrowser.getInstance().browse(topicCompletedLink(nextActivityInfo.topicId))
      }
    }

    NextActivityInfo.NoTopic, NextActivityInfo.NoActivity -> showNoNextActivityNotification(task, project)
    is NextActivityInfo.NotCalculated -> {
      showNoNextActivityNotification(task, project)
      LOG.warn(
        "Next step for taskId=${task?.id}, topicId=${nextActivityInfo.topicId} isn't calculated yet: current step with not " +
        "completed state is returned"
      )
    }

    is NextActivityInfo.Next -> {
      val nextStep = nextActivityInfo.stepSource
      val topicId = nextActivityInfo.topicId
      if (!HyperskillCourse.isStepSupported(nextStep.block?.name)) {
        EduBrowser.getInstance().browse(stepLink(nextStep.id))
        LOG.warn("Step is not supported: next stepId ${nextStep.id}, current task: ${task?.id} topic: $topicId ")
      }

      val course = project.course ?: return
      val language = HyperskillLanguages.getRequestLanguage(course.languageId) ?: return
      ProjectOpener.getInstance().open(
        HyperskillOpenInIdeRequestHandler,
        HyperskillOpenStepWithProjectRequest(course.id, nextStep.id, language)
      ).onError {
        logger<ProjectOpener>().warn("Opening the next activity resulted in an error: ${it.message}. The error was ignored and not displayed for the user.")
      }
    }
  }
}

private fun getTopic(taskId: Int): Int? {
  val stepSource = HyperskillConnector.getInstance().getStepSource(taskId).onError {
    LOG.warn(it)
    null
  }

  return stepSource?.topic
}

private fun getNextStep(taskId: Int): NextActivityInfo {
  val topicId = getTopic(taskId)
  if (topicId == null) {
    LOG.warn("Topic id is null for a step $taskId")
    return NextActivityInfo.NoTopic
  }

  return getNextStepInTopic(topicId, taskId)
}

private fun getNextStepInTopic(topicId: Int, taskId: Int?): NextActivityInfo {
  val steps = HyperskillConnector.getInstance().getStepsForTopic(topicId).onError { error ->
    LOG.warn(error)
    null
  }

  if (steps.isNullOrEmpty()) {
    return NextActivityInfo.NoActivity
  }

  // it can happen as the next task is calculated asynchronously,
  // But as it never happens during testing, I'll log it to see if we need to process this case
  val currentStep = steps.find { it.id == taskId }
  if (currentStep?.isCompleted == false) {
    return NextActivityInfo.NotCalculated(topicId)
  }

  val nextStep = steps.find { it.isNext }

  return if (nextStep == null) {
    NextActivityInfo.TopicCompleted(topicId)
  }
  else {
    NextActivityInfo.Next(nextStep, topicId)
  }
}

private fun showNoNextActivityNotification(task: Task?, project: Project) {
  EduNotificationManager.create(
    ERROR,
    EduCoreBundle.message("notification.hyperskill.no.next.activity.title"),
    task?.let { EduCoreBundle.message("notification.hyperskill.no.next.activity.content", stepLink(task.id)) } ?: "",
  ).addAction(NotificationAction.createSimpleExpiring("Open in Browser") {
    task?.let { EduBrowser.getInstance().browse(stepLink(it.id)) }
  })
    .notify(project)
}

fun <T : WithPaginationMetaData> withPageIteration(fetchData: (Int) -> Result<T, String>): Result<List<T>, String> {
  val acc = mutableListOf<T>()
  var page = 1

  do {
    val result = fetchData(page++).onError { return Err(it) }
    acc.add(result)
  }
  while (result.meta.hasNext)

  return Ok(acc.toList())
}

private sealed class NextActivityInfo {
  class TopicCompleted(val topicId: Int) : NextActivityInfo()

  data object NoActivity : NextActivityInfo()

  data object NoTopic : NextActivityInfo()

  class Next(val stepSource: HyperskillStepSource, val topicId: Int) : NextActivityInfo()

  class NotCalculated(val topicId: Int) : NextActivityInfo()
}

fun getUnsupportedTaskDescriptionText(name: String, stepId: Int): String {
  val fixedTaskName = name.lowercase().replaceFirstChar { it.titlecaseChar() }
  return EduCoreBundle.message("hyperskill.unsupported.task.description.text", fixedTaskName, stepLink(stepId), HYPERSKILL)
}

fun HyperskillCourse.createTopicsSection(): Section {
  val section = Section()
  section.name = HYPERSKILL_TOPICS
  section.index = items.size + 1
  section.parent = this
  addSection(section)
  return section
}

fun Section.createTopicLesson(name: String): Lesson {
  val lesson = Lesson()
  lesson.name = name
  lesson.index = this.items.size + 1
  lesson.parent = this
  addLesson(lesson)
  return lesson
}
