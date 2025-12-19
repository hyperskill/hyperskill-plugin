package org.hyperskill.academy.learning.courseFormat.hyperskill

import org.hyperskill.academy.learning.capitalize
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROBLEMS
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_PROJECTS_URL
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.HYPERSKILL_TOPICS
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.wrapWithUtm
import java.util.concurrent.ConcurrentHashMap

class HyperskillCourse : Course {

  constructor()

  var taskToTopics: MutableMap<Int, List<HyperskillTopic>> = ConcurrentHashMap()
  var stages: List<HyperskillStage> = mutableListOf()
  var hyperskillProject: HyperskillProject? = null
    set(value) {
      field = value
      id = value?.id ?: 0
    }

  var selectedStage: Int? = null
  var selectedProblem: Int? = null

  constructor(
    hyperskillProject: HyperskillProject,
    languageId: String,
    languageVersion: String?,
    environment: String,
  ) {
    this.hyperskillProject = hyperskillProject
    name = hyperskillProject.title
    description = hyperskillProject.description + descriptionNote(hyperskillProject.id)
    this.languageId = languageId
    this.languageVersion = languageVersion
    this.environment = environment
    id = hyperskillProject.id
  }

  constructor(languageName: String, languageId: String, languageVersion: String?, environment: String = DEFAULT_ENVIRONMENT) {
    name = "${languageName.capitalize()} Problems"
    description = message("hyperskill.problems.project.description", languageName.capitalize())
    this.languageId = languageId
    this.languageVersion = languageVersion
    this.environment = environment
  }

  val isTemplateBased: Boolean
    get() {
      return (hyperskillProject ?: error("Disconnected $HYPERSKILL project")).isTemplateBased
    }


  fun getProjectLesson(): FrameworkLesson? = lessons.firstOrNull() as? FrameworkLesson

  /**
   * For backward compatibility with old course format where Hyperskill problems
   * were stored in [HYPERSKILL_PROBLEMS] lesson.
   * For new code, prefer using [getTopicsSection].
   */
  fun getProblemsLesson(): Lesson? = getLesson { it.presentableName == HYPERSKILL_PROBLEMS }

  /**
   * Hyperskill problems are grouped by their topics. Topics are lessons located in [HYPERSKILL_TOPICS] section.
   *
   * Structure example:
   *
   * [HYPERSKILL_TOPICS] section
   *
   *    `The for-loop` lesson
   *
   *        `Arithmetic average` task
   *
   *        `Size of parts` task
   *
   *        etc
   *
   *     `Thread synchronization` lesson
   *
   *        `Thread-safe account` task
   *
   *        `Countdown counter` task
   *
   *        etc
   *
   */
  fun getTopicsSection(): Section? = getSection { it.presentableName == HYPERSKILL_TOPICS }

  fun getProblem(id: Int): Task? {
    getTopicsSection()?.lessons?.forEach { lesson ->
      lesson.getTask(id)?.let {
        return it
      }
    }
    return null
  }

  fun isTaskInProject(task: Task): Boolean = task.lesson == getProjectLesson()

  fun isTaskInTopicsSection(task: Task): Boolean = getTopicsSection()?.lessons?.contains(task.lesson) == true

  private fun descriptionNote(projectId: Int): String {
    val link = "$HYPERSKILL_PROJECTS_URL/$projectId"
    return """<br/><br/>${message("learn.more.at")} <a href="${wrapWithUtm(link, "project-card")}">$link</a>"""
  }

  override val itemType: String = HYPERSKILL

  // lexicographical order
  companion object {
    private val SUPPORTED_STEP_TYPES: Set<String> = setOf(
      HyperskillTaskType.CODE.type,
      HyperskillTaskType.PYCHARM.type,
      HyperskillTaskType.TEXT.type,
    )

    fun isStepSupported(type: String?): Boolean = type in SUPPORTED_STEP_TYPES
  }
}
