package org.hyperskill.academy.learning.courseFormat

object EduFormatNames {
  const val REST_PREFIX = "api"
  const val CODE_ARGUMENT = "code"

  const val COURSE_ICON_FILE = "courseIcon.svg"

  const val COURSE = "course"
  const val SECTION = "section"
  const val LESSON = "lesson"
  const val FRAMEWORK = "framework"
  const val TASK = "task"
  const val ITEM = "item"

  // vendor
  const val EMAIL = "email"
  const val NAME = "name"
  const val URL = "url"

  // Submissions status
  const val CORRECT = "correct"
  const val WRONG = "wrong"
  const val UNCHECKED = "unchecked"

  // attempt
  const val TIME_LEFT = "time_left"
  const val ID = "id"
  const val TIME = "time"
  const val STEP = "step"
  const val DATASET = "dataset"
  const val STATUS = "status"
  const val USER = "user"

  // dataset
  const val IS_MULTIPLE_CHOICE = "is_multiple_choice"
  const val OPTIONS = "options"
  const val PAIRS = "pairs"
  const val ROWS = "rows"
  const val COLUMNS = "columns"
  const val IS_CHECKBOX = "is_checkbox"

  const val DEFAULT_ENVIRONMENT = ""

  // Used as course type only
  const val PYCHARM = "PyCharm"

  //hyperskill
  const val TITLE = "title"
  const val THEORY_ID = "theory"
  const val STEP_ID = "step"
  const val IS_COMPLETED = "is_completed"
  const val DESCRIPTION = "description"
  const val IDE_FILES = "ide_files"
  const val USE_IDE = "use_ide"
  const val LANGUAGE = "language"
  const val ENVIRONMENT = "environment"
  const val IS_TEMPLATE_BASED = "is_template_based"
  const val HYPERSKILL_PROBLEMS = "Problems"
  const val HYPERSKILL_TOPICS = "Topics"
  const val TOPICS = "topics"
  const val HYPERSKILL_PROJECTS_URL = "https://hyperskill.org/projects"
  const val HYPERSKILL = "Hyperskill"

  // stepik
  const val ATTEMPT = "attempt"
  const val CHECK_PROFILE = "check_profile"

  // IDs of supported languages. They are the same that `Language#getID` returns
  // but in some cases we don't have corresponding Language in classpath to get its id via `getID` method
  const val JAVA = "JAVA"
  const val KOTLIN = "kotlin"
  const val PYTHON = "Python"
  const val SCALA = "Scala"
  const val JAVASCRIPT = "JavaScript"
  const val SHELL = "Shell Script"

  // Single `ObjectiveC` id is used both for `ObjectiveC` and `C/C++`
  const val CPP = "ObjectiveC"
  const val GO = "go"
  const val CSHARP = "C#"

  const val PYTHON_2_VERSION = "2.x"
  const val PYTHON_3_VERSION = "3.x"

  // Troubleshooting guide links
  const val TROUBLESHOOTING_GUIDE_URL = "https://support.hyperskill.org/hc/en-us/sections/20682551792404-IDE-Troubleshooting"
  const val NO_TESTS_URL = "https://support.hyperskill.org/hc/en-us/articles/28831366886932-Error-No-test-have-run-in-PyCharm"
  const val FAILED_TO_CHECK_URL = TROUBLESHOOTING_GUIDE_URL

  val LOGIN_NEEDED_MESSAGE = message("check.error.login.needed")
}
