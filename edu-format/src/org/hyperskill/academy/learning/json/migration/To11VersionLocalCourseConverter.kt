package org.hyperskill.academy.learning.json.migration

import com.fasterxml.jackson.databind.node.ObjectNode
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.json.migration.MigrationNames.ANDROID
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.COURSE_TYPE
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.ENVIRONMENT

class To11VersionLocalCourseConverter : JsonLocalCourseConverterBase() {

  override fun convert(localCourse: ObjectNode): ObjectNode {
    val courseType = localCourse.get(COURSE_TYPE)?.asText() ?: EduFormatNames.PYCHARM
    if (courseType == ANDROID) {
      localCourse.put(ENVIRONMENT, ANDROID)
      localCourse.put(COURSE_TYPE, EduFormatNames.PYCHARM)
    }
    return localCourse
  }
}
