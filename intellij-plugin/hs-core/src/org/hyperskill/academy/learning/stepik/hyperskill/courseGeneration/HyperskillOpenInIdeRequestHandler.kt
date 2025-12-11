package org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration

import com.intellij.openapi.application.runInEdt
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.notification.EduNotificationManager
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import org.hyperskill.academy.learning.authUtils.requestFocus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.KOTLIN
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.courseFormat.ext.*
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTaskType
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ALL_FILES
import org.hyperskill.academy.learning.courseGeneration.OpenInIdeRequestHandler
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.navigation.NavigationUtils.navigateToTask
import org.hyperskill.academy.learning.stepik.hyperskill.*
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillSolutionLoader
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillStepSource
import org.hyperskill.academy.learning.yaml.YamlFormatSynchronizer
import org.jetbrains.annotations.VisibleForTesting

object HyperskillOpenInIdeRequestHandler : OpenInIdeRequestHandler<HyperskillOpenRequest>() {
  private val LOG = Logger.getInstance(HyperskillOpenInIdeRequestHandler::class.java)

  private const val STAGE_LOADING_TIMEOUT_MS = 60_000L

  override val courseLoadingProcessTitle: String get() = EduCoreBundle.message("hyperskill.loading.project")

  private inline fun <T> logTimed(operation: String, block: () -> T): T {
    LOG.info("$operation: started")
    val startTime = System.currentTimeMillis()
    try {
      return block()
    }
    finally {
      LOG.info("$operation: completed in ${System.currentTimeMillis() - startTime}ms")
    }
  }

  private fun hasAndroidEnvironment(course: Course): Boolean = course.environment == EduNames.ANDROID

  /**
   * Loads stages with a timeout to prevent IDE from hanging on slow networks.
   * Returns Ok if successful, Err with error message on failure or timeout.
   * Rethrows ProcessCanceledException if the operation was cancelled.
   */
  private fun loadStagesWithTimeout(
    project: Project,
    hyperskillCourse: HyperskillCourse,
    timeoutMs: Long = STAGE_LOADING_TIMEOUT_MS
  ): Result<Unit, String> {
    val future = CompletableFuture.supplyAsync {
      computeUnderProgress(project, EduCoreBundle.message("hyperskill.loading.stages")) {
        HyperskillConnector.getInstance().loadStages(hyperskillCourse)
      }
    }

    return try {
      future.get(timeoutMs, TimeUnit.MILLISECONDS)
      Ok(Unit)
    }
    catch (e: TimeoutException) {
      future.cancel(true)
      LOG.warn("Stage loading timed out after ${timeoutMs}ms")
      Err(EduCoreBundle.message("hyperskill.error.stage.loading.timeout"))
    }
    catch (e: CancellationException) {
      LOG.info("Stage loading was cancelled")
      throw ProcessCanceledException()
    }
    catch (e: ExecutionException) {
      val cause = e.cause
      // Rethrow ProcessCanceledException - it's a control flow exception
      if (cause is ProcessCanceledException) {
        throw cause
      }
      LOG.warn("Failed to load stages: ${cause?.message ?: e.message}", e)
      Err(EduCoreBundle.message("hyperskill.error.stage.loading.failed", cause?.message ?: e.message ?: "Unknown error"))
    }
    catch (e: InterruptedException) {
      LOG.info("Stage loading was interrupted")
      Thread.currentThread().interrupt()
      throw ProcessCanceledException()
    }
  }

  /**
   * Shows an error notification when stage loading fails.
   */
  private fun showStageLoadingError(project: Project, errorMessage: String) {
    EduNotificationManager.showErrorNotification(
      project = project,
      title = EduCoreBundle.message("hyperskill.error.stage.loading.title"),
      content = errorMessage
    )
  }

  override fun openInExistingProject(
    request: HyperskillOpenRequest,
    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?
  ): Project? {
    when (request) {
      is HyperskillOpenStepRequestBase -> {
        val stepId = request.stepId
        val stepSource = getStepSource(stepId, request.isLanguageSelectedByUser)
        val isAndroidEnvRequired = stepSource.framework == EduNames.ANDROID
        val courseFilter: (Course) -> Boolean = if (isAndroidEnvRequired) ::hasAndroidEnvironment else { _ -> true }
        val (project, course) = findExistingProject(findProject, request, courseFilter) ?: return null
        val hyperskillCourse = course as HyperskillCourse
        hyperskillCourse.addProblemsWithTopicWithFiles(project, stepSource)
        hyperskillCourse.selectedProblem = stepId
        runInEdt {
          requestFocus()
          navigateToStep(project, hyperskillCourse, stepId)
        }
        synchronizeProjectOnStepOpening(project, hyperskillCourse, stepId)
        return project
      }

      is HyperskillOpenProjectStageRequest -> {
        LOG.info("Opening project stage request: projectId=${request.projectId}, stageId=${request.stageId}")
        val (project, course) = findExistingProject(findProject, request) ?: run {
          LOG.info("No existing project found for projectId=${request.projectId}")
          return null
        }
        LOG.info("Found existing project: ${project.name}")
        val hyperskillCourse = course as HyperskillCourse
        if (hyperskillCourse.getProjectLesson() == null) {
          LOG.info("Project lesson not found, loading stages")

          // Load stages with timeout to prevent IDE from hanging
          val loadResult = logTimed("Loading stages with timeout") {
            loadStagesWithTimeout(project, hyperskillCourse)
          }

          if (loadResult is Err) {
            LOG.warn("Stage loading failed: ${loadResult.error}")
            showStageLoadingError(project, loadResult.error)
            // Return the project so user can retry later, but show error notification
            course.selectedStage = request.stageId
            runInEdt { openSelectedStage(hyperskillCourse, project) }
            return project
          }

          logTimed("Course init") { hyperskillCourse.init(false) }
          val projectLesson = hyperskillCourse.getProjectLesson() ?: run {
            LOG.warn("Project lesson is null after loading stages")
            showStageLoadingError(project, EduCoreBundle.message("hyperskill.error.stage.loading.no.lesson"))
            return project
          }
          val courseDir = project.courseDir
          logTimed("Creating lesson directory") { GeneratorUtils.createLesson(project, projectLesson, courseDir) }
          logTimed("Unpacking additional files") { GeneratorUtils.unpackAdditionalFiles(CourseInfoHolder.fromCourse(course, courseDir), ALL_FILES) }
          logTimed("Saving YAML files") { YamlFormatSynchronizer.saveAll(project) }
          logTimed("Refreshing project") { course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.DEPENDENCIES_UPDATED) }
          synchronizeProjectOnStageOpening(project, hyperskillCourse, projectLesson.taskList)
        }
        else {
          LOG.info("Project lesson already exists, skipping stages loading")
        }
        course.selectedStage = request.stageId
        runInEdt { openSelectedStage(hyperskillCourse, project) }
        LOG.info("Project stage opening completed")
        return project
      }

    }
  }

  private fun navigateToStep(project: Project, course: Course, stepId: Int) {
    if (stepId == 0) {
      return
    }
    val task = getTask(course, stepId) ?: return
    navigateToTask(project, task)
  }

  private fun getTask(course: Course, stepId: Int): Task? {
    val taskRef = Ref<Task>()
    course.visitLessons { lesson: Lesson ->
      val task = lesson.getTask(stepId) ?: return@visitLessons
      taskRef.set(task)
    }
    return taskRef.get()
  }

  private fun findExistingProject(
    findProject: ((Course) -> Boolean) -> Pair<Project, Course>?,
    request: HyperskillOpenRequest,
    courseFilter: (Course) -> Boolean = { true }
  ): Pair<Project, Course>? {
    return when (request) {
      is HyperskillOpenProjectStageRequest -> findProject { it.matchesById(request.projectId) }
      is HyperskillOpenStepWithProjectRequest -> {
        val hyperskillLanguage = request.language
        val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage) ?: return null

        findProject {
          it.matchesById(request.projectId) && it.languageId == languageId && it.languageVersion == languageVersion && courseFilter(it)
        }
        ?: findProject { course -> course.isHyperskillProblemsCourse(hyperskillLanguage) }
      }

      is HyperskillOpenStepRequest -> {
        val hyperskillLanguage = request.language
        val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage) ?: return null
        findProject { it is HyperskillCourse && it.languageId == languageId && it.languageVersion == languageVersion && courseFilter(it) }
        ?: findProject { course -> course.isHyperskillProblemsCourse(hyperskillLanguage) && courseFilter(course) }
      }
    }
  }

  private fun Course.isHyperskillProblemsCourse(hyperskillLanguage: String) =
    this is HyperskillCourse && name in listOf(
      getProblemsProjectName(hyperskillLanguage),
      getLegacyProblemsProjectName(hyperskillLanguage)
    )

  private fun Course.matchesById(projectId: Int) = this is HyperskillCourse && hyperskillProject?.id == projectId

  fun createHyperskillCourse(
    request: HyperskillOpenRequest,
    hyperskillLanguage: String,
    hyperskillProject: HyperskillProject
  ): Result<HyperskillCourse, CourseValidationResult> {
    val (languageId, languageVersion) = HyperskillLanguages.getLanguageIdAndVersion(hyperskillLanguage)
                                        ?: return Err(
                                          ValidationErrorMessage(
                                            EduCoreBundle.message(
                                              "hyperskill.unsupported.language",
                                              hyperskillLanguage
                                            )
                                          )
                                        )

    if (!hyperskillProject.useIde) {
      return Err(ValidationErrorMessageWithHyperlinks(EduCoreBundle.message("hyperskill.project.not.supported", HYPERSKILL_PROJECTS_URL)))
    }

    val eduEnvironment = hyperskillProject.eduEnvironment
                         ?: return Err(ValidationErrorMessage("Unsupported environment ${hyperskillProject.environment}"))

    if (request is HyperskillOpenStepWithProjectRequest) {
      // This condition is about opening e.g. Python problem with chosen Kotlin's project,
      // otherwise - open Kotlin problem in current Kotlin project itself later below
      if (hyperskillLanguage != hyperskillProject.language) {
        return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))
      }

      // This is about opening Kotlin problem with currently chosen Android's project
      // But all Android projects are always Kotlin one's
      // So it should be possible to open problem in IntelliJ IDEA too e.g. (EDU-4641)
      if (eduEnvironment == EduNames.ANDROID && hyperskillLanguage == KOTLIN) {
        return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))
      }

      // EDU-5994: if Step has a field 'framework' == 'Android', it should be opened only in Android Studio
      // as an Android project.
      val stepSource = getStepSource(request.stepId, request.isLanguageSelectedByUser)
      // When the user has selected project on Hyperskill, the Plugin has to check if it is an Android project,
      // if not: a new one should be created.
      if (stepSource.framework == EduNames.ANDROID && eduEnvironment != EduNames.ANDROID) {
        if (!EduUtilsKt.isAndroidStudio()) {
          return Err(ValidationErrorMessageWithHyperlinks(EduCoreBundle.message("rest.service.android.not.supported")))
        }
        return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion, EduNames.ANDROID))
      }

    }
    if (request is HyperskillOpenStepRequest) return Ok(HyperskillCourse(hyperskillLanguage, languageId, languageVersion))

    // Android projects must be opened in Android Studio only
    if (eduEnvironment == EduNames.ANDROID && !EduUtilsKt.isAndroidStudio()) {
      return Err(ValidationErrorMessageWithHyperlinks(EduCoreBundle.message("rest.service.android.not.supported")))
    }
    val hyperskillCourse = HyperskillCourseCreator.createHyperskillCourse(hyperskillProject, languageId, languageVersion, eduEnvironment)
    return Ok(hyperskillCourse)
  }

  override fun getCourse(request: HyperskillOpenRequest, indicator: ProgressIndicator): Result<Course, CourseValidationResult> {
    LOG.info("getCourse called for request: ${request.javaClass.simpleName}")
    val totalStartTime = System.currentTimeMillis()

    // Set up progress indicator for better user feedback
    indicator.isIndeterminate = false
    indicator.fraction = 0.0

    if (request is HyperskillOpenStepRequest) {
      LOG.info("Processing HyperskillOpenStepRequest: stepId=${request.stepId}, language=${request.language}")
      indicator.text2 = EduCoreBundle.message("hyperskill.loading.step.info")
      indicator.fraction = 0.2

      val newProject = HyperskillProject()
      val hyperskillLanguage = request.language
      val hyperskillCourse = createHyperskillCourse(request, hyperskillLanguage, newProject).onError { return Err(it) }

      indicator.fraction = 0.5
      indicator.text2 = EduCoreBundle.message("hyperskill.loading.problems")
      hyperskillCourse.addProblemsWithTopicWithFiles(null, getStepSource(request.stepId, request.isLanguageSelectedByUser))
      hyperskillCourse.selectedProblem = request.stepId

      indicator.fraction = 1.0
      LOG.info("HyperskillOpenStepRequest processed in ${System.currentTimeMillis() - totalStartTime}ms")
      return Ok(hyperskillCourse)
    }

    request as HyperskillOpenWithProjectRequestBase
    LOG.info("Fetching project info for projectId=${request.projectId}")
    indicator.text2 = EduCoreBundle.message("hyperskill.loading.project.info")
    indicator.fraction = 0.1

    val projectStartTime = System.currentTimeMillis()
    val hyperskillProject = HyperskillConnector.getInstance().getProject(request.projectId).onError {
      LOG.warn("Failed to fetch project ${request.projectId}: $it")
      return Err(ValidationErrorMessage(it))
    }
    LOG.info("Project info fetched in ${System.currentTimeMillis() - projectStartTime}ms")

    indicator.fraction = 0.3
    indicator.text2 = EduCoreBundle.message("hyperskill.creating.course")

    val hyperskillLanguage = if (request is HyperskillOpenStepWithProjectRequest) request.language else hyperskillProject.language
    LOG.info("Creating course for language: $hyperskillLanguage")

    val hyperskillCourse = createHyperskillCourse(request, hyperskillLanguage, hyperskillProject).onError {
      LOG.warn("Failed to create course: $it")
      return Err(it)
    }

    hyperskillCourse.validateLanguage(hyperskillLanguage).onError {
      LOG.warn("Language validation failed: $it")
      return Err(it)
    }

    indicator.fraction = 0.5

    when (request) {
      is HyperskillOpenStepWithProjectRequest -> {
        LOG.info("Loading problems for stepId=${request.stepId}")
        indicator.text2 = EduCoreBundle.message("hyperskill.loading.problems")
        hyperskillCourse.addProblemsWithTopicWithFiles(null, getStepSource(request.stepId, request.isLanguageSelectedByUser))
        hyperskillCourse.selectedProblem = request.stepId
      }

      is HyperskillOpenProjectStageRequest -> {
        LOG.info("Loading stages for new project, stageId=${request.stageId}")
        indicator.text2 = EduCoreBundle.message("hyperskill.loading.stages")
        ProgressManager.checkCanceled()
        HyperskillConnector.getInstance().loadStages(hyperskillCourse)
        hyperskillCourse.selectedStage = request.stageId
      }
    }

    indicator.fraction = 1.0
    LOG.info("getCourse completed in ${System.currentTimeMillis() - totalStartTime}ms total")
    return Ok(hyperskillCourse)
  }

  @VisibleForTesting
  fun getStepSource(stepId: Int, isLanguageSelectedByUser: Boolean): HyperskillStepSource {
    val connector = HyperskillConnector.getInstance()
    val stepSource = connector.getStepSource(stepId).onError { error(it) }

    // Choosing language by user is allowed only for Data tasks, see EDU-4718
    if (isLanguageSelectedByUser) {
      error("Language has been selected by user not for data task, but it must be specified for other tasks in request")
    }
    return stepSource
  }

  private fun Lesson.addProblems(stepSources: List<HyperskillStepSource>): Result<List<Task>, String> {
    val existingTasksIds = items.map { it.id }
    val stepsSourceForAdding = stepSources.filter { it.id !in existingTasksIds }

    val tasks = HyperskillConnector.getTasks(course, stepsSourceForAdding)
    tasks.forEach(this::addTask)
    return Ok(tasks)
  }

  private fun HyperskillStepSource.getTopicWithRecommendedSteps(): Result<Pair<String, List<HyperskillStepSource>>, String> {
    val connector = HyperskillConnector.getInstance()
    val topicId = topic ?: return Err("Topic must not be null")

    val stepSources = connector.getStepsForTopic(topicId)
      .onError { return Err(it) }
      .filter { it.isRecommended || it.id == id }.toMutableList()

    val theoryTask = stepSources.find { it.block?.name == HyperskillTaskType.TEXT.type }
    if (theoryTask != null) {
      stepSources.remove(theoryTask)
      stepSources.add(0, theoryTask)
    }

    val theoryTitle = stepSources.find { it.block?.name == HyperskillTaskType.TEXT.type }?.title
    if (theoryTitle != null) {
      return Ok(Pair(theoryTitle, stepSources))
    }

    LOG.warn("Can't get theory step title for $id step")
    val problemTitle = title
    return Ok(Pair(problemTitle, stepSources))
  }

  @VisibleForTesting
  fun HyperskillCourse.addProblemsWithTopicWithFiles(project: Project?, stepSource: HyperskillStepSource): Result<Unit, String> {
    return computeUnderProgress(title = EduCoreBundle.message("hyperskill.loading.problems")) {
      var localTopicsSection = getTopicsSection()
      val createSectionDir = localTopicsSection == null
      if (localTopicsSection == null) {
        localTopicsSection = createTopicsSection()
      }

      val (topicNameSource, stepSources) = stepSource.getTopicWithRecommendedSteps().onError { return@computeUnderProgress Err(it) }
      var localTopicLesson = localTopicsSection.getLesson { it.presentableName == topicNameSource }
      val createLessonDir = localTopicLesson == null
      if (localTopicLesson == null) {
        localTopicLesson = localTopicsSection.createTopicLesson(topicNameSource)
      }

      val tasks = localTopicLesson.addProblems(stepSources).onError { return@computeUnderProgress Err(it) }
      localTopicsSection.init(this, false)

      if (project != null) {
        when {
          createSectionDir -> saveSectionDir(project, course, localTopicsSection, localTopicLesson, tasks)
          createLessonDir -> saveLessonDir(project, localTopicsSection, localTopicLesson, tasks)
          else -> saveTasks(project, localTopicLesson, tasks)
        }

        if (tasks.isNotEmpty()) {
          course.configurator?.courseBuilder?.refreshProject(project, RefreshCause.STRUCTURE_MODIFIED)
        }
      }
      Ok(Unit)
    }
  }

  private fun saveSectionDir(
    project: Project,
    course: Course,
    topicsSection: Section,
    topicLesson: Lesson,
    tasks: List<Task>
  ) {
    GeneratorUtils.createSection(project, topicsSection, project.courseDir)
    tasks.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) }
    YamlFormatSynchronizer.saveItem(topicLesson)
    YamlFormatSynchronizer.saveItem(topicsSection)
    YamlFormatSynchronizer.saveItem(course)
  }

  private fun saveLessonDir(
    project: Project,
    topicSection: Section,
    topicLesson: Lesson,
    tasks: List<Task>
  ) {
    val parentDir = topicSection.getDir(project.courseDir) ?: error("Can't get directory of Topics section")
    GeneratorUtils.createLesson(project, topicLesson, parentDir)
    tasks.forEach { task -> YamlFormatSynchronizer.saveItemWithRemoteInfo(task) }
    YamlFormatSynchronizer.saveItem(topicLesson)
    YamlFormatSynchronizer.saveItem(topicSection)
  }

  private fun saveTasks(
    project: Project,
    topicLesson: Lesson,
    tasks: List<Task>,
  ) {
    tasks.forEach { task ->
      topicLesson.getDir(project.courseDir)?.let { lessonDir ->
        GeneratorUtils.createTask(project, task, lessonDir)
        YamlFormatSynchronizer.saveItemWithRemoteInfo(task)
      }
    }
    YamlFormatSynchronizer.saveItem(topicLesson)
  }

  private fun synchronizeProjectOnStepOpening(project: Project, course: HyperskillCourse, stepId: Int) {
    if (isUnitTestMode) {
      return
    }

    val task = course.getProblem(stepId) ?: return
    val tasks = task.lesson.taskList
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
  }

  private fun synchronizeProjectOnStageOpening(project: Project, course: HyperskillCourse, tasks: List<Task>) {
    if (isUnitTestMode) {
      return
    }
    HyperskillSolutionLoader.getInstance(project).loadSolutionsInBackground(course, tasks, true)
    synchronizeTopics(project, course)
  }
}