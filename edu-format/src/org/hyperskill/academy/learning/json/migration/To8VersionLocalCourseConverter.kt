package org.hyperskill.academy.learning.json.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.json.migration.MigrationNames.ANDROID
import org.hyperskill.academy.learning.json.migration.MigrationNames.KOTLIN
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.PROGRAMMING_LANGUAGE

class To8VersionLocalCourseConverter : JsonLocalCourseConverter {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    val language = localCourse.get(PROGRAMMING_LANGUAGE)?.asText() ?: ""
    var courseType = localCourse.get(COURSE_TYPE)?.asText() ?: EduFormatNames.PYCHARM
    if ("edu-android" == language) {
      localCourse.put(PROGRAMMING_LANGUAGE, KOTLIN)
      courseType = ANDROID
    }
    localCourse.put(COURSE_TYPE, courseType)

    return localCourse
  }
}
