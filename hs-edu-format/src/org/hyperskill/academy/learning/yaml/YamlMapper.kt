package org.hyperskill.academy.learning.yaml

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.jsontype.NamedType
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.dataformat.yaml.YAMLParser
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.kotlinModule
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.attempts.DataTaskAttempt
import org.hyperskill.academy.learning.courseFormat.CheckFeedback
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillProject
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillStage
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillTopic
import org.hyperskill.academy.learning.courseFormat.tasks.CodeTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.yaml.format.*
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.HYPERSKILL_TYPE_YAML
import org.hyperskill.academy.learning.yaml.format.hyperskill.HyperskillCourseMixin
import org.hyperskill.academy.learning.yaml.format.hyperskill.HyperskillProjectMixin
import org.hyperskill.academy.learning.yaml.format.hyperskill.HyperskillStageMixin
import org.hyperskill.academy.learning.yaml.format.hyperskill.HyperskillTopicMixin
import org.hyperskill.academy.learning.yaml.format.remote.DataTaskAttemptYamlMixin
import org.hyperskill.academy.learning.yaml.format.remote.RemoteCourseYamlMixin
import org.hyperskill.academy.learning.yaml.format.remote.RemoteStudyItemYamlMixin
import org.hyperskill.academy.learning.yaml.format.student.FeedbackYamlMixin
import org.hyperskill.academy.learning.yaml.format.student.StudentTaskFileYamlMixin
import org.hyperskill.academy.learning.yaml.format.student.StudentTaskYamlMixin
import org.hyperskill.academy.learning.yaml.format.tasks.CodeTaskYamlMixin
import org.hyperskill.academy.learning.yaml.format.tasks.TaskYamlMixin
import org.hyperskill.academy.learning.yaml.format.tasks.TheoryTaskYamlUtil
import java.util.*

object YamlMapper {
  const val CURRENT_YAML_VERSION = 5

  fun basicMapper(): ObjectMapper {
    val mapper = createMapper()
    mapper.addMixIns()
    return mapper
  }

  fun remoteMapper(): ObjectMapper {
    val mapper = createMapper()
    addRemoteMixIns(mapper)
    return mapper
  }

  private fun createMapper(): ObjectMapper {
    val yamlFactory = YAMLFactory.builder()
      .disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)
      .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID)
      .enable(YAMLGenerator.Feature.MINIMIZE_QUOTES)
      .enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE)
      .enable(YAMLParser.Feature.EMPTY_STRING_AS_NULL)
      .build()

    return JsonMapper.builder(yamlFactory)
      .addModule(kotlinModule())
      .addModule(JavaTimeModule())
      .defaultLocale(Locale.ENGLISH)
      .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
      .serializationInclusion(JsonInclude.Include.NON_EMPTY)
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      // In some cases (f.e. deserialization of course): we deserialize YAML content as a tree and then try to cast it to a necessary object.
      // Therefore, some properties (such as `course.languageVersion`) could be interpreted as float during a deserialization.
      // This can lead to removing trailing zeros and incorrect deserialization. Thus, we disable this behavior implicitly.
      .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
      .disable(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES)
      .disable(MapperFeature.AUTO_DETECT_FIELDS, MapperFeature.AUTO_DETECT_GETTERS, MapperFeature.AUTO_DETECT_IS_GETTERS)
      .build()
  }

  private fun ObjectMapper.addMixIns() {
    addMixIn(HyperskillCourse::class.java, RemoteCourseYamlMixin::class.java)
    addMixIn(Course::class.java, CourseYamlMixin::class.java)
    addMixIn(Section::class.java, SectionYamlMixin::class.java)
    addMixIn(Lesson::class.java, LessonYamlMixin::class.java)
    addMixIn(FrameworkLesson::class.java, FrameworkLessonYamlMixin::class.java)
    addMixIn(Task::class.java, StudentTaskYamlMixin::class.java)
    addMixIn(CodeTask::class.java, CodeTaskYamlMixin::class.java)
    addMixIn(TheoryTask::class.java, TheoryTaskYamlUtil::class.java)
    addMixIn(CheckFeedback::class.java, FeedbackYamlMixin::class.java)
    addMixIn(EduFile::class.java, AdditionalFileYamlMixin::class.java)
    addMixIn(TaskFile::class.java, StudentTaskFileYamlMixin::class.java)

    registerSubtypes(NamedType(HyperskillCourse::class.java, HYPERSKILL_TYPE_YAML))
  }

  private fun addRemoteMixIns(mapper: ObjectMapper) {
    mapper.addMixIn(Lesson::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(Section::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(Task::class.java, RemoteStudyItemYamlMixin::class.java)
    mapper.addMixIn(DataTaskAttempt::class.java, DataTaskAttemptYamlMixin::class.java)
    mapper.addHyperskillMixins()
  }

  private fun ObjectMapper.addHyperskillMixins() {
    addMixIn(HyperskillCourse::class.java, HyperskillCourseMixin::class.java)
    addMixIn(HyperskillProject::class.java, HyperskillProjectMixin::class.java)
    addMixIn(HyperskillStage::class.java, HyperskillStageMixin::class.java)
    addMixIn(HyperskillTopic::class.java, HyperskillTopicMixin::class.java)
  }

}