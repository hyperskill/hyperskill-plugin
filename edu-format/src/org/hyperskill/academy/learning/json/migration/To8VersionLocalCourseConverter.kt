package org.hyperskill.academy.learning.json.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.json.migration.MigrationNames.ANDROID
import org.hyperskill.academy.learning.json.migration.MigrationNames.KOTLIN
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE_ID

class To8VersionLocalCourseConverter : JsonLocalCourseConverter {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    // Read from PROGRAMMING_LANGUAGE for backward compatibility in this migration converter
    val language = localCourse.get(PROGRAMMING_LANGUAGE)?.asText() ?: ""
    var courseType = localCourse.get(COURSE_TYPE)?.asText() ?: EduFormatNames.PYCHARM

    if ("edu-android" == language) {
      // Update both old and new fields for compatibility
      localCourse.put(PROGRAMMING_LANGUAGE, KOTLIN)
      localCourse.put(PROGRAMMING_LANGUAGE_ID, KOTLIN)
      // No version specified for Kotlin in this migration
      courseType = ANDROID
    }
    else if (language.isNotEmpty()) {
      // If we have a language in the old field but not in the new one, copy it
      if (localCourse.get(PROGRAMMING_LANGUAGE_ID) == null) {
        localCourse.put(PROGRAMMING_LANGUAGE_ID, language)
      }
    }

    localCourse.put(COURSE_TYPE, courseType)

    return localCourse
  }
}
