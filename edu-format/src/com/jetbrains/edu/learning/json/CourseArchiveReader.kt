@file:JvmName("CourseArchiveReader")

package com.jetbrains.edu.learning.json

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.InjectableValues
import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.node.ObjectNode
import com.jetbrains.edu.learning.courseFormat.*
import com.jetbrains.edu.learning.courseFormat.tasks.Task
import com.jetbrains.edu.learning.json.encrypt.EncryptionModule
import com.jetbrains.edu.learning.json.migration.*
import com.jetbrains.edu.learning.json.mixins.*
import com.jetbrains.edu.learning.json.mixins.JsonMixinNames.VERSION
import org.jetbrains.annotations.VisibleForTesting
import java.text.SimpleDateFormat
import java.util.*


fun migrate(jsonObject: ObjectNode): ObjectNode {
  return migrate(jsonObject, JSON_FORMAT_VERSION)
}

@VisibleForTesting
fun migrate(node: ObjectNode, maxVersion: Int): ObjectNode {
  var jsonObject = node
  val jsonVersion = jsonObject.get(VERSION)
  var version = jsonVersion?.asInt() ?: 1

  while (version < maxVersion) {
    var converter: JsonLocalCourseConverter? = null
    when (version) {
      6 -> converter = ToSeventhVersionLocalCourseConverter()
      7 -> converter = To8VersionLocalCourseConverter()
      8 -> converter = To9VersionLocalCourseConverter()
      9 -> converter = To10VersionLocalCourseConverter()
      10 -> converter = To11VersionLocalCourseConverter()
      11 -> converter = To12VersionLocalCourseConverter()
    }
    if (converter != null) {
      jsonObject = converter.convert(jsonObject)
    }
    version++
  }
  return jsonObject
}

fun getCourseMapper(fileContentsFactory: FileContentsFactory): ObjectMapper {
  return JsonMapper.builder()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    .disable(MapperFeature.AUTO_DETECT_FIELDS)
    .disable(MapperFeature.AUTO_DETECT_GETTERS)
    .disable(MapperFeature.AUTO_DETECT_IS_GETTERS)
    .disable(MapperFeature.AUTO_DETECT_CREATORS)
    .injectableValues(
      InjectableValues.Std(
        mapOf(
          FILE_CONTENTS_FACTORY_INJECTABLE_VALUE to fileContentsFactory
        )
      )
    )
    .setDateFormat()
    .build()
}

fun ObjectMapper.configureCourseMapper(isEncrypted: Boolean) {
  if (isEncrypted) {
    registerModule(EncryptionModule())
  }
  val module = SimpleModule()
  module.addDeserializer(StudyItem::class.java, StudyItemDeserializer())
  module.addDeserializer(Course::class.java, CourseDeserializer())
  registerModule(module)

  addStudyItemMixins()
}

fun ObjectMapper.addStudyItemMixins() {
  addMixIn(PluginInfo::class.java, PluginInfoMixin::class.java)
  addMixIn(Section::class.java, RemoteSectionMixin::class.java)
  addMixIn(FrameworkLesson::class.java, RemoteFrameworkLessonMixin::class.java)
  addMixIn(Lesson::class.java, RemoteLessonMixin::class.java)
  addMixIn(Task::class.java, RemoteTaskMixin::class.java)
  addMixIn(TaskFile::class.java, TaskFileMixin::class.java)
  addMixIn(EduFile::class.java, EduFileMixin::class.java)
  addMixIn(AnswerPlaceholder::class.java, AnswerPlaceholderWithAnswerMixin::class.java)
  addMixIn(AnswerPlaceholderDependency::class.java, AnswerPlaceholderDependencyMixin::class.java)
}

fun JsonMapper.Builder.setDateFormat(): JsonMapper.Builder {
  val mapperDateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.ENGLISH)
  mapperDateFormat.timeZone = TimeZone.getTimeZone("UTC")
  return defaultDateFormat(mapperDateFormat)
}
