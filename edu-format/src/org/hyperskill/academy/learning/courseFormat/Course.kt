package org.hyperskill.academy.learning.courseFormat

import org.hyperskill.academy.learning.courseFormat.EduFormatNames.DEFAULT_ENVIRONMENT
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.PYCHARM
import java.util.*

/**
 * To introduce new course it's required to:
 * - Extend Course class
 * - Update CourseBuilder#build() in [org.hyperskill.academy.learning.yaml.format.CourseYamlUtil] to handle course loading from YAML
 * - Override [Course.itemType], that's how we find appropriate [org.hyperskill.academy.learning.configuration.EduConfigurator]
 */
abstract class Course : LessonContainer() {
  var description: String = ""
  var environment: String = DEFAULT_ENVIRONMENT
  var environmentSettings: Map<String, String> = mapOf() // here we store a map with keys understandable by specific course builders
  var courseMode: CourseMode = CourseMode.STUDENT //this field is used to distinguish study and course creator modes
  var solutionsHidden: Boolean = false
  var disabledFeatures: List<String> = emptyList()

  @Transient
  var visibility: CourseVisibility = CourseVisibility.LocalVisibility

  var additionalFiles: List<EduFile> = emptyList()

  @Transient
  var pluginDependencies: List<PluginInfo> = emptyList()

  @Transient
  private val nonEditableFiles: MutableSet<String> = mutableSetOf()

  @Transient
  var authors: List<UserInfo> = emptyList()

  /**
   * Not intended to be used to check if it's a local course, needed to pass info for course creation
   * to check if course is local use CCUtilsUtils.kt#Project.isLocalCourse
   */
  @Transient
  var isLocal: Boolean = false

  /**
   * Whether YAML files for tasks (task-info.yaml) should have 'text' fields with the contents of task files.
   * Normally, the file contents should not be written in YAML.
   * But it used to be written before, and we leave here a switch to select either the old or the modern behaviour.
   *
   * The better place to control YAML serialization is in Jackson mappers, but currently there are too many places where mappers are
   * created, so it will take a lot of effort to control all that mappers.
   */
  @Transient
  var needWriteYamlText: Boolean = false

  open var languageCode: String = "en"

  /**
   * Specifies the path to sections/lessons relative to the course
   */
  var customContentPath: String = ""

  /**
   * Programming language ID from [com.intellij.lang.Language.getID]
   * also see [org.hyperskill.academy.learning.courseFormat.ext.CourseExt.getLanguageById]
   */
  open var languageId: String = ""

  /**
   * Programming language versions in string format
   * also see [org.hyperskill.academy.learning.EduNames.PYTHON_2_VERSION], [org.hyperskill.academy.learning.EduNames.PYTHON_3_VERSION]
   */
  open var languageVersion: String? = null

  fun init(isRestarted: Boolean) {
    init(this, isRestarted)
  }

  override fun init(parentItem: ItemContainer, isRestarted: Boolean) {
    require(parentItem is Course)
    super.init(parentItem, isRestarted)
  }

  fun getLesson(sectionName: String?, lessonName: String): Lesson? {
    if (sectionName != null) {
      val section = getSection(sectionName)
      if (section != null) {
        return section.getLesson(lessonName)
      }
    }
    return lessons.firstOrNull { lessonName == it.name }
  }

  val sections: List<Section>
    get() = items.filterIsInstance<Section>()

  fun addSection(section: Section) {
    addItem(section)
  }

  fun removeSection(toRemove: Section) {
    removeItem(toRemove)
  }

  fun getSection(name: String): Section? {
    return getSection { name == it.name }
  }

  fun getSection(predicate: (Section) -> Boolean): Section? {
    return sections.firstOrNull { predicate(it) }
  }

  override val course: Course
    get() = this

  override val itemType: String = PYCHARM //"PyCharm" is used here for historical reasons

  val isStudy: Boolean
    get() = CourseMode.STUDENT == courseMode

  override fun sortItems() {
    super.sortItems()
    sections.forEach { it.sortItems() }
  }

  override fun toString(): String {
    return name
  }

  val authorFullNames: List<String>
    get() {
      return authors.map { it.getFullName() }
    }

  open val humanLanguage: String
    get() = Locale.forLanguageTag(languageCode).displayName

  open val isStepikRemote: Boolean
    get() = false

  fun isEditableFile(path: String): Boolean {
    return !nonEditableFiles.contains(path)
  }

  fun addNonEditableFile(path: String?) {
    if (path != null && isStudy) {
      nonEditableFiles.add(path)
    }
  }

  fun removeNonEditableFile(path: String?) {
    if (path != null && isStudy) {
      nonEditableFiles.remove(path)
    }
  }
}
