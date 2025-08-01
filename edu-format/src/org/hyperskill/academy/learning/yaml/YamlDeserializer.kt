package org.hyperskill.academy.learning.yaml

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.MissingNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.CourseMode.Companion.toCourseMode
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.*
import org.hyperskill.academy.learning.courseFormat.tasks.CodeTask.Companion.CODE_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask.Companion.EDU_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.IdeTask.Companion.IDE_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.OutputTask.Companion.OUTPUT_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask.Companion.REMOTE_EDU_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask.Companion.THEORY_TASK_TYPE
import org.hyperskill.academy.learning.courseFormat.tasks.UnsupportedTask.Companion.UNSUPPORTED_TASK_TYPE
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.COURSE_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.LESSON_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_COURSE_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_LESSON_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_SECTION_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.REMOTE_TASK_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.SECTION_CONFIG
import org.hyperskill.academy.learning.yaml.YamlConfigSettings.TASK_CONFIG
import org.hyperskill.academy.learning.yaml.YamlMapper.basicMapper
import org.hyperskill.academy.learning.yaml.YamlMapper.remoteMapper
import org.hyperskill.academy.learning.yaml.errorHandling.formatError
import org.hyperskill.academy.learning.yaml.errorHandling.loadingError
import org.hyperskill.academy.learning.yaml.errorHandling.unknownConfigMessage
import org.hyperskill.academy.learning.yaml.errorHandling.unsupportedItemTypeMessage
import org.hyperskill.academy.learning.yaml.format.RemoteStudyItem
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.LESSON
import org.hyperskill.academy.learning.yaml.format.YamlMixinNames.TASK
import org.hyperskill.academy.learning.yaml.migrate.YamlMigrator
import org.jetbrains.annotations.VisibleForTesting

/**
 * Deserialize [StudyItem] object from yaml config file without any additional modifications.
 * It means that deserialized object contains only values from corresponding config files which
 * should be applied to existing one that is done in [org.hyperskill.academy.learning.yaml.YamlLoader.loadItem].
 */
object YamlDeserializer {
  fun deserializeItem(
    configName: String,
    mapper: ObjectMapper,
    configFileText: String,
    parentItem: StudyItem?,
    itemFolder: String?
  ): StudyItem {
    return when (configName) {
      COURSE_CONFIG -> mapper.deserializeCourse(configFileText)
      SECTION_CONFIG -> mapper.deserializeSection(configFileText, parentItem as? Course, itemFolder)
      LESSON_CONFIG -> mapper.deserializeLesson(configFileText, parentItem, itemFolder)
      TASK_CONFIG -> mapper.deserializeTask(configFileText, parentItem as? Lesson, itemFolder)
      else -> loadingError(unknownConfigMessage(configName))
    }
  }

  /**
   * Creates [ItemContainer] object from yaml config file.
   * For [Course] object the instance of a proper type is created inside [org.hyperskill.academy.learning.yaml.format.CourseBuilder]
   */
  fun ObjectMapper.deserializeCourse(configFileText: String): Course {
    var treeNode = readTree(configFileText) ?: JsonNodeFactory.instance.objectNode()

    if (treeNode is ObjectNode) {
      treeNode = YamlMigrator(this).migrateCourse(treeNode)
    }

    val course = treeToValue(treeNode, Course::class.java)
    course.courseMode = CourseMode.STUDENT
    return course
  }

  private fun ObjectMapper.readNode(configFileText: String): JsonNode =
    when (val tree = readTree(configFileText)) {
      null -> JsonNodeFactory.instance.objectNode()
      is MissingNode -> JsonNodeFactory.instance.objectNode()
      else -> tree
    }

  @VisibleForTesting
  fun ObjectMapper.deserializeSection(configFileText: String, parentCourse: Course? = null, sectionFolder: String? = null): Section {
    var jsonNode = readNode(configFileText)

    jsonNode = migrateStudyItemYamlTree(jsonNode, parentCourse, sectionFolder) { node, course, folder ->
      migrateSection(node, course, folder)
    }

    return treeToValue(jsonNode, Section::class.java)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeLesson(configFileText: String, parentItem: StudyItem? = null, lessonFolder: String? = null): Lesson {
    var treeNode = readNode(configFileText)

    treeNode = migrateStudyItemYamlTree(treeNode, parentItem, lessonFolder) { node, item, folder ->
      migrateLesson(node, item, folder)
    }

    val type = asText(treeNode.get(YamlMixinNames.TYPE))
    val clazz = when (type) {
      FrameworkLesson().itemType -> FrameworkLesson::class.java
      null, Lesson().itemType -> Lesson::class.java
      else -> formatError(unsupportedItemTypeMessage(type, LESSON))
    }
    return treeToValue(treeNode, clazz)
  }

  @VisibleForTesting
  fun ObjectMapper.deserializeTask(configFileText: String, parentLesson: Lesson? = null, taskFolder: String? = null): Task {
    var treeNode = readNode(configFileText)

    treeNode = migrateStudyItemYamlTree(treeNode, parentLesson, taskFolder) { node, lesson, folder ->
      migrateTask(node, lesson, folder)
    }

    val type = asText(treeNode.get(YamlMixinNames.TYPE))
               ?: formatError(message("yaml.editor.invalid.task.type.not.specified"))

    val clazz = when (type) {
      EDU_TASK_TYPE -> EduTask::class.java
      REMOTE_EDU_TASK_TYPE -> RemoteEduTask::class.java
      OUTPUT_TASK_TYPE -> OutputTask::class.java
      THEORY_TASK_TYPE -> TheoryTask::class.java
      IDE_TASK_TYPE -> IdeTask::class.java
      // for student mode
      CODE_TASK_TYPE -> CodeTask::class.java
      UNSUPPORTED_TASK_TYPE -> UnsupportedTask::class.java
      else -> formatError(unsupportedItemTypeMessage(type, TASK))
    }
    return treeToValue(treeNode, clazz)
  }

  private fun <T : StudyItem> ObjectMapper.migrateStudyItemYamlTree(
    treeNode: JsonNode,
    parentItem: T?,
    itemFolder: String?,
    migrateAction: YamlMigrator.(ObjectNode, T, String) -> ObjectNode
  ): JsonNode {
    val migrator = YamlMigrator(this)

    // treeNode must be ObjectNode, but we don't throw an error right now to get more specific error messages later
    if (treeNode is ObjectNode && migrator.needMigration()) {
      if (parentItem != null && itemFolder != null) {
        return migrator.migrateAction(treeNode, parentItem, itemFolder)
      }
      else {
        LOG.severe("No parent item or item folder is specified during migration")
      }
    }

    return treeNode
  }

  fun deserializeRemoteItem(configName: String, configFileText: String): StudyItem {
    return when (configName) {
      REMOTE_COURSE_CONFIG -> deserializeCourseRemoteInfo(configFileText)
      REMOTE_LESSON_CONFIG -> deserializeLessonRemoteInfo(configFileText)
      REMOTE_SECTION_CONFIG -> remoteMapper().readValue(configFileText, RemoteStudyItem::class.java)
      REMOTE_TASK_CONFIG -> deserializeTaskRemoteInfo(configFileText)
      else -> loadingError(unknownConfigMessage(configName))
    }
  }

  private fun deserializeCourseRemoteInfo(configFileText: String): Course {
    val remoteMapper = remoteMapper()
    val treeNode = remoteMapper.readTree(configFileText)

    val clazz = HyperskillCourse::class.java

    return remoteMapper.treeToValue(treeNode, clazz)
  }

  private fun deserializeLessonRemoteInfo(configFileText: String): StudyItem {
    val treeNode = remoteMapper().readTree(configFileText)
    return remoteMapper().treeToValue(treeNode, RemoteStudyItem::class.java)
  }

  private fun deserializeTaskRemoteInfo(configFileText: String): StudyItem {
    val treeNode = remoteMapper().readTree(configFileText)

    val clazz = RemoteStudyItem::class.java

    return remoteMapper().treeToValue(treeNode, clazz)
  }

  private fun asText(node: JsonNode?): String? {
    return if (node == null || node.isNull) null else node.asText()
  }

  val StudyItem.childrenConfigFileNames: Array<String>
    get() = when (this) {
      is Course -> arrayOf(SECTION_CONFIG, LESSON_CONFIG)
      is Section -> arrayOf(LESSON_CONFIG)
      is Lesson -> arrayOf(TASK_CONFIG)
      else -> error("Unexpected StudyItem: ${javaClass.simpleName}")
    }

  fun getCourseMode(courseConfigText: String): CourseMode? {
    val treeNode = basicMapper().readTree(courseConfigText)
    val courseModeText = asText(treeNode.get(YamlMixinNames.MODE))
    return courseModeText?.toCourseMode()
  }

  private val LOG = logger<YamlDeserializer>()
}
