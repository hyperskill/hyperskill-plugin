package org.hyperskill.academy.learning

import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.runBlockingCancellable
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.LightPlatformTestCase
import org.hyperskill.academy.learning.configuration.PlainTextTaskCheckerProvider.Companion.CHECK_RESULT_FILE
import org.hyperskill.academy.learning.courseFormat.*
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.LESSON
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.SECTION
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.TASK
import org.hyperskill.academy.learning.courseFormat.ext.configurator
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.*
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.IdeaDirectoryUnpackMode.ONLY_IDEA_DIRECTORY
import org.intellij.lang.annotations.Language
import java.util.*

@DslMarker
annotation class CourseDsl

fun course(
  name: String = "Test Course",
  language: com.intellij.lang.Language = PlainTextLanguage.INSTANCE,
  description: String = "Test Course Description",
  environment: String = "",
  courseMode: CourseMode = CourseMode.STUDENT,
  courseProducer: () -> Course = ::HyperskillCourse,
  buildCourse: CourseBuilder.() -> Unit
): Course {
  val builder = CourseBuilder(courseProducer())
  builder.withName(name)
  builder.withMode(courseMode)
  val course = builder.course
  course.languageId = language.id
  course.environment = environment
  course.description = description
  builder.buildCourse()
  return course
}

@Suppress("UnstableApiUsage")
fun Course.createCourseFiles(
  project: Project,
  baseDir: VirtualFile = LightPlatformTestCase.getSourceRoot()
) {
  ProgressManager.getInstance().runProcessWithProgressSynchronously({
    val holder = CourseInfoHolder.fromCourse(this, baseDir)

    runBlockingCancellable {
      configurator
        ?.courseBuilder
        ?.getCourseProjectGenerator(this@createCourseFiles)
        ?.createCourseStructure(holder)
    }

    GeneratorUtils.unpackAdditionalFiles(holder, ONLY_IDEA_DIRECTORY)
  }, "Course Structure Generation", false, project)
}

@CourseDsl
abstract class LessonOwnerBuilder(val course: Course) {

  protected abstract val nextLessonIndex: Int
  protected abstract fun addLesson(lesson: Lesson)

  fun frameworkLesson(
    name: String? = null,
    customPresentableName: String? = null,
    isTemplateBased: Boolean = true,
    id: Int = 0,
    buildLesson: LessonBuilder<FrameworkLesson>.() -> Unit = {}
  ) {
    val lesson = FrameworkLesson().also { it.isTemplateBased = isTemplateBased }
    lesson(lesson, name, customPresentableName, id, buildLesson = buildLesson)
  }

  fun lesson(
    name: String? = null,
    customPresentableName: String? = null,
    id: Int = 0,
    index: Int? = null,
    updateDate: Date = Date(0),
    buildLesson: LessonBuilder<Lesson>.() -> Unit = {}
  ) {
    lesson(Lesson(), name, customPresentableName, id, index, updateDate, buildLesson)
  }

  protected fun <T : Lesson> lesson(
    lesson: T,
    name: String? = null,
    customPresentableName: String? = null,
    id: Int = 0,
    index: Int? = null,
    updateDate: Date = Date(0),
    buildLesson: LessonBuilder<T>.() -> Unit
  ) {
    val lessonBuilder = LessonBuilder(course, null, lesson)
    lesson.index = index ?: nextLessonIndex
    lesson.updateDate = updateDate
    lessonBuilder.withName(name ?: (LESSON + nextLessonIndex))
    lessonBuilder.withCustomPresentableName(customPresentableName)
    lessonBuilder.withId(id)
    addLesson(lesson)
    lessonBuilder.buildLesson()
  }

  fun additionalFiles(buildEduFiles: AdditionalFilesBuilder.() -> Unit) {
    val builder = AdditionalFilesBuilder(course)
    builder.buildEduFiles()
  }
}

class CourseBuilder(course: Course) : LessonOwnerBuilder(course) {

  override val nextLessonIndex: Int get() = course.lessons.size + 1

  fun withName(name: String) {
    course.name = name
  }

  fun withMode(courseMode: CourseMode) {
    course.courseMode = courseMode
  }

  override fun addLesson(lesson: Lesson) {
    course.addLesson(lesson)
  }

  fun section(
    name: String? = null,
    customPresentableName: String? = null,
    id: Int = 0,
    index: Int? = null,
    buildSection: SectionBuilder.() -> Unit = {}
  ) {
    val sectionBuilder = SectionBuilder(course, Section())
    val section = sectionBuilder.section
    val nextSectionIndex = course.items.size + 1
    section.index = index ?: nextSectionIndex
    sectionBuilder.withName(name ?: (SECTION + nextSectionIndex))
    sectionBuilder.withCustomPresentableName(customPresentableName)
    sectionBuilder.withId(id)
    course.addSection(section)
    sectionBuilder.buildSection()
  }

  fun additionalFile(name: String, text: String = "", buildTaskFile: EduFileBuilder.() -> Unit = {}) =
    additionalFile(name, InMemoryUndeterminedContents(text), buildTaskFile)

  fun additionalFile(name: String, contents: FileContents, buildTaskFile: EduFileBuilder.() -> Unit = {}) {
    val builder = EduFileBuilder()
    builder.withName(name)
    builder.withContents(contents)
    builder.buildTaskFile()

    course.additionalFiles = course.additionalFiles + builder.eduFile
  }
}

class SectionBuilder(course: Course, val section: Section = Section()) : LessonOwnerBuilder(course) {
  init {
    section.parent = course
  }

  override val nextLessonIndex: Int get() = section.lessons.size + 1

  override fun addLesson(lesson: Lesson) {
    section.addLesson(lesson)
  }

  fun withName(name: String) {
    section.name = name
  }

  fun withCustomPresentableName(name: String?) {
    section.customPresentableName = name
  }

  fun withId(id: Int) {
    section.id = id
  }
}

@CourseDsl
class LessonBuilder<T : Lesson>(val course: Course, section: Section?, val lesson: T) {

  init {
    lesson.parent = section ?: course
  }

  fun withName(name: String) {
    lesson.name = name
  }

  fun withCustomPresentableName(name: String?) {
    lesson.customPresentableName = name
  }

  fun withId(id: Int) {
    lesson.id = id
  }

  private fun task(
    task: Task,
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    // we want to know task files order in tests
    task.taskFiles = LinkedHashMap()
    val taskBuilder = TaskBuilder(lesson, task)
    taskBuilder.task.index = lesson.taskList.size + 1
    val nextTaskIndex = lesson.taskList.size + 1
    taskBuilder.withName(name ?: (TASK + nextTaskIndex))
    taskBuilder.withCustomPresentableName(customPresentableName)
    val descriptionFormat = if (task.course is HyperskillCourse) DescriptionFormat.HTML else taskDescriptionFormat
    taskBuilder.withTaskDescription(taskDescription ?: "solve task", descriptionFormat)
    taskBuilder.withStepId(stepId)
    taskBuilder.withUpdateDate(updateDate)
    taskBuilder.buildTask()
    for ((_, taskFile) in taskBuilder.task.taskFiles) {
      taskFile.isVisible = taskBuilder.explicitVisibility[taskFile.name] ?: !EduUtilsKt.isTestsFile(taskBuilder.task, taskFile.name)
    }

    lesson.addTask(taskBuilder.task)
  }

  fun eduTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(EduTask(), name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun remoteEduTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    checkProfile: String = "",
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val remoteEduTask = RemoteEduTask()
    task(remoteEduTask, name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)
    remoteEduTask.checkProfile = checkProfile
  }

  fun theoryTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(TheoryTask(), name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun outputTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(OutputTask(), name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun codeTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    taskDescriptionFormat: DescriptionFormat? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) = task(CodeTask(), name, customPresentableName, taskDescription, taskDescriptionFormat, stepId, updateDate, buildTask)

  fun remoteEduTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val remoteEduTask = RemoteEduTask()
    task(remoteEduTask, name, customPresentableName, taskDescription, DescriptionFormat.HTML, stepId, updateDate, buildTask)
  }

  fun unsupportedTask(
    name: String? = null,
    customPresentableName: String? = null,
    taskDescription: String? = null,
    stepId: Int = 0,
    updateDate: Date = Date(0),
    buildTask: TaskBuilder.() -> Unit = {}
  ) {
    val unsupportedTask = UnsupportedTask()
    task(unsupportedTask, name, customPresentableName, taskDescription, DescriptionFormat.HTML, stepId, updateDate, buildTask)
  }
}

@CourseDsl
class TaskBuilder(val lesson: Lesson, val task: Task) {

  private val _explicitVisibility: MutableMap<String, Boolean> = mutableMapOf()

  val explicitVisibility: Map<String, Boolean> get() = _explicitVisibility

  init {
    task.parent = lesson
  }

  fun withName(name: String) {
    task.name = name
  }

  fun withCustomPresentableName(name: String?) {
    task.customPresentableName = name
  }

  fun withTaskDescription(text: String, format: DescriptionFormat? = null) {
    task.descriptionText = text
    task.descriptionFormat = format ?: DescriptionFormat.MD
  }

  fun withUpdateDate(date: Date) {
    task.updateDate = date
  }

  fun withStepId(stepId: Int) {
    task.id = stepId
  }

  /**
   * Creates task file with given [name] and textual contents [text].
   *
   * You can also create placeholders for this task file using `<p>` tag.
   *
   * For example, for `fun foo() = <p>TODO()</p>` text
   * it creates task file with `fun foo() = TODO()` text and placeholder with `TODO()` as placeholder text.
   */
  fun taskFile(
    name: String, text: String,
    visible: Boolean? = null,
    editable: Boolean? = true,
    propagatable: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, editable, propagatable, buildTaskFile)

  /**
   * Creates task file with given [name] and [contents].
   *
   * If the contents is textual, you can also create placeholders for this task file using `<p>` tag.
   *
   * For example, for `fun foo() = <p>TODO()</p>` text
   * it creates task file with `fun foo() = TODO()` text and placeholder with `TODO()` as placeholder text.
   */
  fun taskFile(
    name: String, contents: FileContents = UndeterminedContents.EMPTY,
    visible: Boolean? = null,
    editable: Boolean? = true,
    propagatable: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) {
    val taskFileBuilder = TaskFileBuilder(task)
    taskFileBuilder.withName(name)

    if (contents !is BinaryContents) {
      val textBuilder = StringBuilder(contents.textualRepresentation.trimIndent())
      taskFileBuilder.withContents(InMemoryTextualContents(textBuilder.toString()))
    }
    else {
      taskFileBuilder.withContents(contents)
    }

    taskFileBuilder.buildTaskFile()
    val taskFile = taskFileBuilder.taskFile
    if (visible != null) {
      _explicitVisibility[name] = visible
    }
    taskFile.task = task
    taskFile.isEditable = editable ?: true
    taskFile.isPropagatable = propagatable ?: !EduUtilsKt.isTestsFile(task, taskFile.name)
    task.addTaskFile(taskFile)
  }

  fun checkResultFile(status: CheckStatus, message: String = "") {
    checkResultFile("$status $message")
  }

  fun checkResultFile(message: String) {
    taskFile(CHECK_RESULT_FILE, message)
  }

  fun kotlinTaskFile(
    name: String,
    @Language("kotlin") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun javaTaskFile(
    name: String,
    @Language("JAVA") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun pythonTaskFile(
    name: String,
    @Language("Python") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun scalaTaskFile(
    name: String,
    @Language("Scala") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun rustTaskFile(
    name: String,
    @Language("Rust") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun goTaskFile(
    name: String,
    @Language("Go") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun cppTaskFile(
    name: String,
    @Language("ObjectiveC") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun xmlTaskFile(
    name: String,
    @Language("XML") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun sqlTaskFile(
    name: String,
    @Language("SQL") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun javaScriptTaskFile(
    name: String,
    @Language("JavaScript") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun phpTaskFile(
    name: String,
    @Language("PHP") text: String = "",
    visible: Boolean? = null,
    buildTaskFile: TaskFileBuilder.() -> Unit = {}
  ) = taskFile(name, InMemoryTextualContents(text), visible, buildTaskFile = buildTaskFile)

  fun dir(dirName: String, buildTask: TaskBuilder.() -> Unit) {
    val tmpTask = EduTask()
    val innerBuilder = TaskBuilder(lesson, tmpTask)
    innerBuilder.buildTask()
    for ((_, taskFile) in tmpTask.taskFiles) {
      val visibility = innerBuilder.explicitVisibility[taskFile.name]
      taskFile.name = "$dirName/${taskFile.name}"
      task.addTaskFile(taskFile)
      if (visibility != null) {
        _explicitVisibility[taskFile.name] = visibility
      }
    }
  }

}

@CourseDsl
class AdditionalFilesBuilder(val course: Course) {

  fun eduFile(
    name: String, text: String = "",
    buildEduFile: EduFileBuilder.() -> Unit = {}
  ) {
    val eduFileBuilder = EduFileBuilder()
    eduFileBuilder.withName(name)
    val textBuilder = StringBuilder(text.trimIndent())
    eduFileBuilder.withContents(InMemoryTextualContents(textBuilder.toString()))
    eduFileBuilder.buildEduFile()
    val taskFile = eduFileBuilder.eduFile
    course.additionalFiles = course.additionalFiles + taskFile
  }

}

@CourseDsl
class TaskFileBuilder(val task: Task? = null) {
  val taskFile = TaskFile()

  init {
    if (task != null) {
      taskFile.task = task
    }
  }

  fun withName(name: String) {
    taskFile.name = name
  }

  fun withText(text: String) {
    taskFile.contents = InMemoryTextualContents(text)
  }

  fun withContents(contents: FileContents) {
    taskFile.contents = contents
  }
}

@CourseDsl
class EduFileBuilder {
  val eduFile = EduFile()

  fun withName(name: String) {
    eduFile.name = name
  }

  fun withText(text: String) {
    eduFile.contents = InMemoryTextualContents(text)
  }

  fun withContents(contents: FileContents) {
    eduFile.contents = contents
  }
}
