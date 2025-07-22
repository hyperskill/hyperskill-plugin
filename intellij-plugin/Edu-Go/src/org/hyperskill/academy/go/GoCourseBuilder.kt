package org.hyperskill.academy.go

import com.goide.sdk.GoSdk
import com.intellij.openapi.project.Project
import org.hyperskill.academy.coursecreator.StudyItemType
import org.hyperskill.academy.coursecreator.StudyItemType.TASK_TYPE
import org.hyperskill.academy.coursecreator.actions.TemplateFileInfo
import org.hyperskill.academy.coursecreator.actions.studyItem.NewStudyItemInfo
import org.hyperskill.academy.go.GoConfigurator.Companion.GO_MOD
import org.hyperskill.academy.go.GoConfigurator.Companion.MAIN_GO
import org.hyperskill.academy.go.GoConfigurator.Companion.TASK_GO
import org.hyperskill.academy.go.GoConfigurator.Companion.TEST_GO
import org.hyperskill.academy.go.messages.EduGoBundle
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.DefaultSettingsUtils.findPath
import org.hyperskill.academy.learning.EduNames.TEST
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.joinPaths
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator

class GoCourseBuilder : EduCourseBuilder<GoProjectSettings> {

  override fun getCourseProjectGenerator(course: Course): CourseProjectGenerator<GoProjectSettings> =
    GoCourseProjectGenerator(this, course)

  override fun getLanguageSettings(): LanguageSettings<GoProjectSettings> = GoLanguageSettings()

  override fun getDefaultSettings(): Result<GoProjectSettings, String> {
    return findPath(DEFAULT_GO_SDK_PROPERTY, "Go sdk").flatMap { sdkPath ->
      val sdk = GoSdk.fromHomePath(sdkPath)
      when {
        sdk == GoSdk.NULL -> Err("Can't find Go sdk in `$sdkPath`")
        !sdk.isValid -> Err("`$sdkPath` contains invalid Go sdk")
        else -> Ok(GoProjectSettings(sdk))
      }
    }
  }

  override fun getTestTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> {
    val templates = mutableListOf(TemplateFileInfo(TEST_GO, joinPaths(TEST, TEST_GO), false))
    if (withSources) {
      templates += TemplateFileInfo(EDU_TASK_TEMPLATE, TASK_GO, true)
      templates += TemplateFileInfo(EDU_MAIN_TEMPLATE, joinPaths("main", MAIN_GO), true)
      templates += TemplateFileInfo(GO_MOD, GO_MOD, false)
    }
    return templates
  }

  override fun getExecutableTaskTemplates(course: Course, info: NewStudyItemInfo, withSources: Boolean): List<TemplateFileInfo> {
    if (!withSources) return emptyList()
    return listOf(
      TemplateFileInfo(MAIN_GO, MAIN_GO, true),
      TemplateFileInfo(GO_MOD, GO_MOD, false)
    )
  }

  override fun extractInitializationParams(info: NewStudyItemInfo): Map<String, String> {
    val moduleName = info.name.replace(" ", "_").lowercase()
    val moduleQuoted = "\"$moduleName\""
    return mapOf(
      "MODULE_NAME" to moduleName,
      "MAIN_IMPORTS" to createImportsSection(FMT, "task $moduleQuoted"),
      "TEST_IMPORTS" to createImportsSection(TESTING, "task $moduleQuoted")
    )
  }

  // https://golang.org/ref/spec#Import_declarations
  override fun validateItemName(project: Project, name: String, itemType: StudyItemType): String? {
    return if (itemType == TASK_TYPE && name.contains(FORBIDDEN_SYMBOLS)) EduGoBundle.message("error.invalid.name") else null
  }

  /**
   * We have 2 types of imports in Go:
   * 1. "import/without/spaces"
   * 2. alias_without_spaces "import/without/spaces"
   * All imports should be sorted by value in quotes.
   * */
  private fun createImportsSection(vararg imports: String): String =
    imports.sortedBy { it.split(" ").last() }.joinToString("\n\t")

  companion object {
    val FORBIDDEN_SYMBOLS = """[!"#$%&'()*,:;<=>?\[\]^`{|}~]+""".toRegex()
    private const val TESTING = "\"testing\""
    private const val FMT = "\"fmt\""

    private const val EDU_TASK_TEMPLATE = "edu_task.go"
    private const val EDU_MAIN_TEMPLATE = "edu_main.go"

    private const val DEFAULT_GO_SDK_PROPERTY = "project.go.sdk"
  }
}
