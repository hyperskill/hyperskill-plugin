@file:Suppress("unused", "PropertyName", "MemberVisibilityCanBePrivate")

package org.hyperskill.academy.learning.stepik.api

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonProperty
import com.intellij.openapi.diagnostic.logger
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.ATTEMPT
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.CHECK_PROFILE
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.NAME
import org.hyperskill.academy.learning.courseFormat.JSON_FORMAT_VERSION
import org.hyperskill.academy.learning.courseFormat.TaskFile
import org.hyperskill.academy.learning.courseFormat.attempts.Attempt
import org.hyperskill.academy.learning.courseFormat.attempts.AttemptBase
import org.hyperskill.academy.learning.stepik.hyperskill.api.WithPaginationMetaData
import org.hyperskill.academy.learning.submissions.SolutionFile
import org.hyperskill.academy.learning.submissions.Submission
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.SOLUTIONS_HIDDEN

const val USERS = "users"
const val META = "meta"
const val COURSES = "courses"
const val LESSONS = "lessons"
const val STEP_SOURCES = "step-sources"
const val SUBMISSIONS = "submissions"
const val PROGRESSES = "progresses"
const val ATTEMPTS = "attempts"
const val ASSIGNMENTS = "assignments"
const val ENROLLMENT = "enrollment"
const val VIEW = "view"
const val UNIT = "unit"
const val STEP_SOURCE = "step_source"
const val MEMBER = "member"
const val USER = "user"
const val GROUP = "group"
const val ASSIGNMENT = "assignment"
const val STEP = "step"
const val IS_PASSED = "is_passed"
const val IS_MULTIPLE_CHOICE = "is_multiple_choice"
const val PAIRS = "pairs"
const val OPTIONS = "options"
const val ORDERING = "ordering"
const val COLUMNS = "columns"
const val NAME_ROW = "name_row"
const val ANSWER = "answer"
const val DATASET = "dataset"
const val REPLY = "reply"
const val HINT = "hint"
const val FEEDBACK = "feedback"
const val MESSAGE = "message"
const val CHOICES = "choices"
const val SCORE = "score"
const val SOLUTION = "solution"
const val CODE = "code"
const val FILE = "file"
const val EDU_TASK = "edu"
const val CODE_TASK = "code"
const val REMOTE_EDU_TASK = "remote_edu"
const val NUMBER_TASK = "number"
const val STRING_TASK = "string"
const val SORTING_BASED_TASK = "sorting_based"
const val CHOICE_TASK = "choice"
const val DATA_TASK = "data"
const val TABLE_TASK = "table"
const val VERSION = "version"
const val ATTACHMENTS = "attachments"
const val COURSE_REVIEW_SUMMARIES = "course-review-summaries"
const val ADDITIONAL_FILES = "additional_files"
const val TASK_FILES = "task_files"
const val TASKS_INFO = "tasks_info"
const val MEMORY = "memory"
const val AVERAGE = "average"
const val FIRST_NAME = "first_name"
const val LAST_NAME = "last_name"
const val IS_GUEST = "is_guest"


class SubmissionsList : WithPaginationMetaData() {
  @JsonProperty(SUBMISSIONS)
  lateinit var submissions: List<StepikBasedSubmission>
}

class AttemptsList : WithPaginationMetaData() {
  @JsonProperty(ATTEMPTS)
  lateinit var attempts: List<Attempt>
}

// Auxiliary:

class Feedback {
  @JsonProperty(MESSAGE)
  var message: String? = null

  constructor()

  constructor(feedback: String) {
    message = feedback
  }
}


open class Reply {
  @JsonProperty(VERSION)
  var version = JSON_FORMAT_VERSION
}

class CodeTaskReply : Reply() {
  @JsonProperty(LANGUAGE)
  var language: String? = null

  @JsonProperty(CODE)
  var code: String? = null
}

class EduTaskReply : Reply() {
  @JsonProperty(FEEDBACK)
  var feedback: Feedback? = null

  @JsonProperty(SCORE)
  var score: String = ""

  @JsonProperty(SOLUTION)
  var solution: List<SolutionFile>? = null

  @JsonProperty(CHECK_PROFILE)
  @JsonInclude(JsonInclude.Include.NON_NULL)
  var checkProfile: String? = null
}

open class AdditionalInfo

class CourseAdditionalInfo : AdditionalInfo {
  @JsonProperty(ADDITIONAL_FILES)
  lateinit var additionalFiles: List<EduFile>

  @JsonProperty(SOLUTIONS_HIDDEN)
  var solutionsHidden: Boolean = false

  constructor()

  constructor(additionalFiles: List<EduFile>, solutionsHidden: Boolean = false) {
    this.additionalFiles = additionalFiles
    this.solutionsHidden = solutionsHidden
  }
}

class LessonAdditionalInfo : AdditionalInfo {
  @JsonProperty(ADDITIONAL_FILES)
  var additionalFiles: List<EduFile> = listOf()

  @JsonProperty(CUSTOM_NAME)
  var customName: String? = null

  /**
   * We have another mechanism to store info about plugin tasks: org.hyperskill.academy.learning.stepik.PyCharmStepOptions
   * This object is used to store additional info about lesson or non-plugin tasks
   * (we use lessonInfo for tasks because Stepik API does not have attachments for tasks)
   * */

  @JsonProperty(TASKS_INFO)
  var tasksInfo: Map<Int, TaskAdditionalInfo> = emptyMap()

  constructor()

  constructor(customName: String?, tasksInfo: Map<Int, TaskAdditionalInfo>, additionalFiles: List<EduFile>) {
    this.customName = customName
    this.tasksInfo = tasksInfo
    this.additionalFiles = additionalFiles
  }

  val isEmpty: Boolean get() = customName.isNullOrEmpty() && tasksInfo.isEmpty() && additionalFiles.isEmpty()
}

// Not inherited from AdditionalInfo because Stepik does not support Attachments for tasks
class TaskAdditionalInfo {
  @JsonProperty(NAME)
  var name: String = ""

  @JsonProperty(CUSTOM_NAME)
  var customName: String? = null

  @JsonProperty(TASK_FILES)
  lateinit var taskFiles: List<TaskFile>

  constructor()

  constructor(name: String, customName: String?, taskFiles: List<TaskFile>) {
    this.name = name
    this.customName = customName
    this.taskFiles = taskFiles
  }
}

class StepikBasedSubmission : Submission {
  @JsonProperty(ATTEMPT)
  var attempt: Int = 0

  @JsonProperty(REPLY)
  var reply: Reply? = null

  @JsonProperty(HINT)
  var hint: String? = null

  @JsonProperty(FEEDBACK)
  var feedback: Feedback? = null

  // WRITE_ONLY because we don't need to send it
  @JsonProperty(STEP, access = JsonProperty.Access.WRITE_ONLY)
  override var taskId: Int = -1

  private val LOG = logger<StepikBasedSubmission>()

  override val solutionFiles: List<SolutionFile>?
    get() {
      val submissionReply = reply
      // https://youtrack.jetbrains.com/issue/EDU-1449
      val solution = (submissionReply as? EduTaskReply)?.solution
      if (submissionReply != null && solution == null) {
        LOG.warn("`solution` field of reply object is null for task $taskId")
      }
      return solution
    }

  override val formatVersion: Int?
    get() = reply?.version

  override fun getSubmissionTexts(taskName: String): Map<String, String>? {
    return if (solutionFiles == null) {
      val submissionText = (reply as? CodeTaskReply)?.code ?: return null
      mapOf(taskName to submissionText)
    }
    else {
      super.getSubmissionTexts(taskName)
    }
  }

  constructor()

  constructor(attempt: AttemptBase, reply: Reply) {
    this.attempt = attempt.id
    this.reply = reply
  }
}
