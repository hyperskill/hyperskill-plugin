package org.hyperskill.academy.learning

import com.fasterxml.jackson.annotation.JsonAlias
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.json.configureCourseMapper
import org.hyperskill.academy.learning.json.getCourseMapper
import org.hyperskill.academy.learning.json.migrate
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.ID
import org.hyperskill.academy.learning.json.mixins.JsonMixinNames.UPDATE_DATE
import org.hyperskill.academy.learning.json.mixins.LocalTaskMixin
import org.hyperskill.academy.learning.json.mixins.RemoteLessonMixin
import org.hyperskill.academy.learning.json.mixins.RemoteSectionMixin
import org.hyperskill.academy.learning.stepik.api.STEPIK_ID
import java.io.File
import java.io.IOException
import java.util.*

@Throws(IOException::class)
fun createCourseFromJson(pathToJson: String): HyperskillCourse {
  val courseJson = File(pathToJson).readText()
  val courseMapper = getCourseMapper(object : FileContentsFactory {
    override fun createBinaryContents(file: EduFile) =
      throw IllegalStateException("description of edu file ${file.pathInCourse} must contain the 'text' field")

    override fun createTextualContents(file: EduFile) =
      throw IllegalStateException("description of edu file ${file.pathInCourse} must contain the 'text' field")
  })
  configureCourseMapper(courseMapper)
  var objectNode = courseMapper.readTree(courseJson) as ObjectNode
  objectNode = migrate(objectNode)
  return courseMapper.treeToValue(objectNode, HyperskillCourse::class.java).apply {
    this.courseMode = CourseMode.STUDENT
  }
}

private fun configureCourseMapper(courseMapper: ObjectMapper) {
  courseMapper.configureCourseMapper()
  courseMapper.addMixIn(Section::class.java, TestRemoteSectionMixin::class.java)
  courseMapper.addMixIn(Lesson::class.java, TestRemoteLessonMixin::class.java)
  courseMapper.addMixIn(Task::class.java, TestRemoteTaskMixin::class.java)
}

@Suppress("unused") // used for correct updateDate deserialization from json test data
abstract class TestRemoteLessonMixin : RemoteLessonMixin() {
  @JsonProperty(UPDATE_DATE)
  private lateinit var updateDate: Date
}

@Suppress("unused") // used for correct updateDate deserialization from json test data
abstract class TestRemoteTaskMixin : LocalTaskMixin() {
  @JsonProperty(ID)
  @JsonAlias(STEPIK_ID)
  private var id: Int = 0

  @JsonProperty(UPDATE_DATE)
  private lateinit var updateDate: Date
}

@Suppress("unused") // used for correct updateDate deserialization from json test data
abstract class TestRemoteSectionMixin : RemoteSectionMixin() {
  @JsonProperty(UPDATE_DATE)
  private lateinit var updateDate: Date
}
