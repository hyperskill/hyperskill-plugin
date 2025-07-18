package com.jetbrains.edu.learning.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.actionSystem.ActionPlaces
import com.jetbrains.edu.learning.authUtils.AuthorizationPlace
import com.jetbrains.edu.learning.courseFormat.CheckStatus
import com.jetbrains.edu.learning.courseFormat.Course
import com.jetbrains.edu.learning.courseFormat.StudyItem
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.newproject.ui.BrowseCoursesDialog
import com.jetbrains.edu.learning.newproject.ui.CoursesPlatformProvider
import com.jetbrains.edu.learning.newproject.ui.myCourses.MyCoursesProvider
import com.jetbrains.edu.learning.statistics.EduCounterUsageCollector.AuthorizationEvent.*
import com.jetbrains.edu.learning.statistics.EduFields.COURSE_MODE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.ITEM_TYPE_FIELD
import com.jetbrains.edu.learning.statistics.EduFields.LANGUAGE_FIELD
import com.jetbrains.edu.learning.stepik.hyperskill.newProjectUI.HyperskillPlatformProvider

/**
 * IMPORTANT: if you modify anything in this class, updated whitelist rules should be
 * provided to analytics platform team.
 */
@Suppress("UnstableApiUsage")
class EduCounterUsageCollector : CounterUsagesCollector() {

  override fun getGroup(): EventLogGroup = GROUP

  enum class TaskNavigationPlace {
    TASK_DESCRIPTION_TOOLBAR,
    CHECK_PANEL,
    UNRESOLVED_DEPENDENCY_NOTIFICATION
  }

  enum class LinkType {
    IN_COURSE, EXTERNAL, PSI, JBA, FILE
  }

  /**
   * [LOG_IN] and [LOG_OUT] events describe user _clicked_ link to do log in or log out
   * These events were used before 2022.3 plugin version
   *
   * [LOG_IN_SUCCEED] and [LOG_OUT_SUCCEED] events describe user actually did authorized or logged out
   */
  private enum class AuthorizationEvent {
    @Deprecated("Use LOG_IN_SUCCEED instead")
    LOG_IN,

    @Deprecated("Use LOG_OUT_SUCCEED instead")
    LOG_OUT,
    LOG_IN_SUCCEED, LOG_OUT_SUCCEED
  }

  private enum class HintEvent {
    EXPANDED, COLLAPSED
  }

  private enum class PostCourseEvent {
    UPDATE
  }

  enum class SynchronizeCoursePlace {
    WIDGET,
  }

  @Suppress("unused") //enum values are not mentioned explicitly
  private enum class CourseActionSource(private val actionPlace: String? = null) {
    WELCOME_SCREEN(ActionPlaces.WELCOME_SCREEN),
    MAIN_MENU(ActionPlaces.MAIN_MENU),
    FIND_ACTION(ActionPlaces.ACTION_SEARCH),
    COURSE_SELECTION_DIALOG(BrowseCoursesDialog.ACTION_PLACE),
    UNKNOWN;

    companion object {
      fun fromActionPlace(actionPlace: String): CourseActionSource {
        // it is possible to have action place like "popup@WelcomScreen"
        val actionPlaceParsed = actionPlace.split("@").last()
        return values().firstOrNull { it.actionPlace == actionPlaceParsed } ?: UNKNOWN
      }
    }
  }

  private enum class CourseSelectionViewTab {
    JBA,
    MY_COURSES,
    UNKNOWN;

    companion object {
      fun fromProvider(provider: CoursesPlatformProvider): CourseSelectionViewTab {
        return when (provider) {
          is HyperskillPlatformProvider -> JBA
          is MyCoursesProvider -> MY_COURSES
          else -> UNKNOWN
        }
      }
    }
  }

  enum class UiOnboardingRelaunchLocation {
    MENU_OR_ACTION, TOOLTIP_RESTART_BUTTON
  }

  companion object {
    private const val SOURCE = "source"
    private const val SUCCESS = "success"
    private const val EVENT = "event"
    private const val TYPE = "type"
    private const val EDU_TAB = "tab"
    private const val UI_ONBOARDING_STEP_INDEX = "index"
    private const val UI_ONBOARDING_STEP_KEY = "key"
    private const val UI_ONBOARDING_RELAUNCH_LOCATION = "location"

    private val GROUP = EventLogGroup(
      "educational.counters",
      "The metric is reported in case a user has called the corresponding JetBrains Academy features.",
      23,
    )

    private val TASK_NAVIGATION_EVENT = GROUP.registerEvent(
      "navigate.to.task",
      "The event is recorded in case a user navigates to the next or previous task/stage/problem.",
      enumField<TaskNavigationPlace>(SOURCE)
    )
    private val EDU_PROJECT_CREATED_EVENT = GROUP.registerEvent(
      "edu.project.created",
      "The event is recorded in case a user creates a new course.",
      COURSE_MODE_FIELD,
      ITEM_TYPE_FIELD,
      LANGUAGE_FIELD
    )
    private val EDU_PROJECT_OPENED_EVENT = GROUP.registerEvent(
      "edu.project.opened",
      "The event is recorded in case a user opens an already existing course.",
      COURSE_MODE_FIELD,
      ITEM_TYPE_FIELD
    )
    private val LICK_CLICKED_EVENT = GROUP.registerEvent(
      "link.clicked",
      "The event is recorded in case a user clicks a link within a task text.",
      enumField<LinkType>(TYPE)
    )
    private val AUTHORIZATION_EVENT = GROUP.registerEvent(
      "authorization",
      "The event is recorded in case a user logs in or out on any platform we support.",
      enumField<AuthorizationEvent>(EVENT),
      EventFields.String(
        "platform", listOf(
          "Hyperskill",
          "Stepik",
          "Js_CheckiO",
          "Py_CheckiO",
          "Marketplace"
        )
      ),
      enumField<AuthorizationPlace>(SOURCE)
    )
    private val SHOW_FULL_OUTPUT_EVENT = GROUP.registerEvent(
      "show.full.output",
      "The event is recorded in case a user clicks the Show Full Output link in the check result panel."
    )
    private val PEEK_SOLUTION_EVENT = GROUP.registerEvent(
      "peek.solution",
      "The event is recorded in case a user clicks the Peek Solution link in the check result panel."
    )
    private val LEAVE_FEEDBACK_EVENT = GROUP.registerEvent(
      "leave.feedback",
      "The event is recorded in case a user clicks the Leave Feedback icon in the check result panel."
    )
    private val REVERT_TASK_EVENT = GROUP.registerEvent(
      "revert.task",
      "The event is recorded in case a user successfully resets content of a task."
    )
    private val CHECK_TASK_EVENT = GROUP.registerEvent(
      "check.task",
      "The event is recorded in case a user checks a task in any course.",
      enumField<CheckStatus>("status")
    )

    private val REVIEW_STAGE_TOPICS_EVENT = GROUP.registerEvent(
      "review.stage.topics",
      "The event is recorded in case a user clicks Review Topics for a stage in a JetBrains Academy project."
    )
    private val HINT_CLICKED_EVENT = GROUP.registerEvent(
      "hint",
      "The event is recorded in case a user expands/collapses hints in the Task Description.",
      enumField<HintEvent>(EVENT)
    )

    private val POST_COURSE_EVENT = GROUP.registerEvent(
      "post.course",
      "The event is recorded in case an educator uploads or updates their course on Stepik or Marketplace in Course Creation mode.",
      enumField<PostCourseEvent>(EVENT)
    )
    private val SYNCHRONIZE_COURSE_EVENT = GROUP.registerEvent(
      "synchronize.course",
      "The event is recorded in case a course is synchronized with its latest version.",
      ITEM_TYPE_FIELD,
      enumField<SynchronizeCoursePlace>(SOURCE)
    )

    private val X_DIALOG_SHOWN_EVENT = GROUP.registerEvent(
      "x.dialog.shown",
      "The event is recorded in case a user receives a suggestion to tweet about completing a course or task (e.g., JB Academy project completion).",
      ITEM_TYPE_FIELD,
      LANGUAGE_FIELD
    )
    private val LINKEDIN_DIALOG_SHOWN_EVENT = GROUP.registerEvent(
      "linkedin.dialog.shown",
      "The event is recorded in case a user receives a suggestion to post to LinkedIn about completing a course or task (e.g., JB Academy project completion).",
      ITEM_TYPE_FIELD,
      LANGUAGE_FIELD
    )
    private val COURSE_SELECTION_VIEW_OPENED_EVENT = GROUP.registerEvent(
      "open.course.selection.view",
      "The event is recorded in case a user opens the Course Selection view.",
      enumField<CourseActionSource>(SOURCE)
    )
    private val COURSE_SELECTION_TAB_SELECTED_EVENT = GROUP.registerEvent(
      "select.tab.course.selection.view",
      "The event is recorded in case a user selects a tab in the Course Selection view.",
      enumField<CourseSelectionViewTab>(EDU_TAB)
    )
    private val VIEW_EVENT = GROUP.registerEvent(
      "open.task",
      "The event is recorded in case a user opens any task.",
      COURSE_MODE_FIELD,
      ITEM_TYPE_FIELD
    )
    private val CREATE_NEW_COURSE_CLICK_EVENT = GROUP.registerEvent(
      "create.new.course.clicked",
      "The event is recorded in case a user opens the Create Course dialog.",
      enumField<CourseActionSource>(SOURCE)
    )
    private val CREATE_NEW_FILE_IN_NON_TEMPLATE_BASED_FRAMEWORK_LESSON_BY_LEARNER =
      GROUP.registerEvent(
        "create.new.file.in.non.template.based.framework.lesson.by.learner",
        "The event is recorded in case a user creates a new file in a Non-Template Based Framework Lesson in Learner mode."
      )

    private val UI_ONBOARDING_STARTED = GROUP.registerEvent(
      "ui.onboarding.started",
      """Track when a user clicks the "Next" button to begin the onboarding tour."""
    )
    private val UI_ONBOARDING_SKIPPED = GROUP.registerEvent(
      "ui.onboarding.skipped",
      """Track when a user clicks "Skip" button. The index and the step key are recorded""",
      EventFields.Int(UI_ONBOARDING_STEP_INDEX),
      // the list of step keys is taken from com.jetbrains.edu.uiOnboarding.EduUiOnboardingService.getDefaultStepsOrder
      EventFields.String(UI_ONBOARDING_STEP_KEY, listOf("welcome", "taskDescription", "codeEditor", "checkSolution"))
    )
    private val UI_ONBOARDING_FINISHED = GROUP.registerEvent(
      "ui.onboarding.finished",
      """Track when a user completes the onboarding tour and clicks the final "Finish" button."""
    )
    private val UI_ONBOARDING_RELAUNCHED = GROUP.registerEvent(
      "ui.onboarding.relaunched",
      """Track when a user manually opens the onboarding tour again from an alternative entry point (e.g. Help menu).""",
      enumField<UiOnboardingRelaunchLocation>(UI_ONBOARDING_RELAUNCH_LOCATION)
    )

    fun taskNavigation(place: TaskNavigationPlace) = TASK_NAVIGATION_EVENT.log(place)

    fun eduProjectCreated(course: Course) = EDU_PROJECT_CREATED_EVENT.log(course.courseMode, course.itemType, course.languageId)

    fun eduProjectOpened(course: Course) = EDU_PROJECT_OPENED_EVENT.log(course.courseMode, course.itemType)

    fun studyItemCreatedCC(item: StudyItem) {
    }

    fun linkClicked(linkType: LinkType) = LICK_CLICKED_EVENT.log(linkType)

    @Deprecated("Use logInSucceed instead")
    fun loggedIn(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_IN, platform, place)

    @Deprecated("Use logOutSucceed instead")
    fun loggedOut(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_OUT, platform, place)

    fun logInSucceed(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_IN_SUCCEED, platform, place)

    fun logOutSucceed(platform: String, place: AuthorizationPlace) = AUTHORIZATION_EVENT.log(LOG_OUT_SUCCEED, platform, place)

    fun fullOutputShown() = SHOW_FULL_OUTPUT_EVENT.log()

    fun solutionPeeked() = PEEK_SOLUTION_EVENT.log()

    fun leaveFeedback() = LEAVE_FEEDBACK_EVENT.log()

    fun revertTask() = REVERT_TASK_EVENT.log()

    fun reviewStageTopics() = REVIEW_STAGE_TOPICS_EVENT.log()

    fun checkTask(status: CheckStatus) = CHECK_TASK_EVENT.log(status)

    fun hintExpanded() = HINT_CLICKED_EVENT.log(HintEvent.EXPANDED)

    fun hintCollapsed() = HINT_CLICKED_EVENT.log(HintEvent.COLLAPSED)

    fun updateCourse() = POST_COURSE_EVENT.log(PostCourseEvent.UPDATE)

    fun synchronizeCourse(course: Course, place: SynchronizeCoursePlace) = SYNCHRONIZE_COURSE_EVENT.log(course.itemType, place)

    fun xDialogShown(course: Course) = X_DIALOG_SHOWN_EVENT.log(course.itemType, course.languageId)

    fun linkedInDialogShown(course: Course) = LINKEDIN_DIALOG_SHOWN_EVENT.log(course.itemType, course.languageId)

    fun courseSelectionViewOpened(actionPlace: String) {
      COURSE_SELECTION_VIEW_OPENED_EVENT.log(CourseActionSource.fromActionPlace(actionPlace))
    }

    fun courseSelectionTabSelected(provider: CoursesPlatformProvider) {
      COURSE_SELECTION_TAB_SELECTED_EVENT.log(CourseSelectionViewTab.fromProvider(provider))
    }

    fun createNewFileInNonTemplateBasedFrameworkLessonByLearner() = CREATE_NEW_FILE_IN_NON_TEMPLATE_BASED_FRAMEWORK_LESSON_BY_LEARNER.log()

    fun viewEvent(task: Task?) {
      val course = task?.course ?: return
      VIEW_EVENT.log(course.courseMode, course.itemType)
    }

    fun uiOnboardingStarted() = UI_ONBOARDING_STARTED.log()

    fun uiOnboardingSkipped(index: Int, key: String) = UI_ONBOARDING_SKIPPED.log(index, key)

    fun uiOnboardingFinished() = UI_ONBOARDING_FINISHED.log()

    fun uiOnboardingRelaunched(location: UiOnboardingRelaunchLocation) = UI_ONBOARDING_RELAUNCHED.log(location)
  }
}