package org.hyperskill.academy.learning.yaml

import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.COURSE
import org.hyperskill.academy.learning.courseFormat.Lesson
import org.hyperskill.academy.learning.courseFormat.Section
import org.hyperskill.academy.learning.courseFormat.StudyItem
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.LESSON
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.SECTION
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TASK
import org.jetbrains.annotations.NonNls

object YamlConfigSettings {
  val COURSE_CONFIG = getLocalConfigFileName(COURSE)
  val SECTION_CONFIG = getLocalConfigFileName(SECTION)
  val LESSON_CONFIG = getLocalConfigFileName(LESSON)
  val TASK_CONFIG = getLocalConfigFileName(TASK)

  /**
   * @param itemKind Course/Section/Lesson/Task
   */
  fun getLocalConfigFileName(itemKind: String): String = "$itemKind-info.yaml"

  @NonNls
  const val REMOTE_COURSE_CONFIG = "course-remote-info.yaml"

  @NonNls
  const val REMOTE_SECTION_CONFIG = "section-remote-info.yaml"

  @NonNls
  const val REMOTE_LESSON_CONFIG = "lesson-remote-info.yaml"

  @NonNls
  const val REMOTE_TASK_CONFIG = "task-remote-info.yaml"

  val StudyItem.configFileName: String
    get() = when (this) {
      is Course -> COURSE_CONFIG
      is Section -> SECTION_CONFIG
      is Lesson -> LESSON_CONFIG
      is Task -> TASK_CONFIG
      else -> {
        @NonNls
        val errorMessageToLog = "Unknown StudyItem type: ${javaClass.simpleName}"
        error(errorMessageToLog)
      }
    }

  val StudyItem.remoteConfigFileName: String
    get() = when (this) {
      is Course -> REMOTE_COURSE_CONFIG
      is Section -> REMOTE_SECTION_CONFIG
      is Lesson -> REMOTE_LESSON_CONFIG
      is Task -> REMOTE_TASK_CONFIG
      else -> {
        @NonNls
        val errorMessageToLog = "Unknown StudyItem type: ${javaClass.simpleName}"
        error(errorMessageToLog)
      }
    }
}
