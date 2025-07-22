@file:Suppress("HardCodedStringLiteral")

package org.hyperskill.academy.learning.configurators

import com.intellij.lang.Language
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.checker.TaskChecker
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.EduFile
import org.hyperskill.academy.learning.courseFormat.tasks.EduTask
import org.hyperskill.academy.learning.gradle.GradleConstants.BUILD_GRADLE
import org.hyperskill.academy.learning.gradle.GradleConstants.SETTINGS_GRADLE
import org.hyperskill.academy.learning.newproject.CourseProjectGenerator
import org.hyperskill.academy.learning.newproject.EmptyProjectSettings
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator
import javax.swing.Icon

object FakeGradleBasedLanguage : Language("FakeGradleBasedLanguage")

object FakeGradleFileType : LanguageFileType(FakeGradleBasedLanguage) {
  override fun getIcon(): Icon? = null
  override fun getName(): String = "FakeGradleFileType"
  override fun getDefaultExtension(): String = "kt"
  override fun getDescription(): String = "File type for fake gradle based language"
}

class FakeGradleConfigurator : EduConfigurator<EmptyProjectSettings> {
  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val courseBuilder: FakeGradleCourseBuilder
    get() = FakeGradleCourseBuilder()

  override val testFileName: String
    get() = TEST_FILE_NAME

  override fun getMockFileName(course: Course, text: String): String = TASK_FILE_NAME

  override val taskCheckerProvider
    get() = object : TaskCheckerProvider {
      override fun getEduTaskChecker(task: EduTask, project: Project): TaskChecker<EduTask> {
        return object : TaskChecker<EduTask>(task, project) {
          override fun check(indicator: ProgressIndicator): CheckResult = CheckResult(CheckStatus.Solved, "")
        }
      }
    }

  companion object {
    const val TEST_FILE_NAME = "Tests.kt"
    const val TASK_FILE_NAME = "Task.kt"
  }
}

class FakeGradleHyperskillConfigurator : HyperskillConfigurator<EmptyProjectSettings>(FakeGradleConfigurator())

class FakeGradleCourseBuilder : EduCourseBuilder<EmptyProjectSettings> {
  override fun getLanguageSettings(): LanguageSettings<EmptyProjectSettings> = object : LanguageSettings<EmptyProjectSettings>() {
    override fun getSettings(): EmptyProjectSettings = EmptyProjectSettings
  }

  override fun getCourseProjectGenerator(course: Course): FakeGradleCourseProjectGenerator = FakeGradleCourseProjectGenerator(
    this, course
  )

  override fun refreshProject(project: Project, cause: RefreshCause) {}
  override fun mainTemplateName(course: Course): String = "Main.kt"
  override fun testTemplateName(course: Course): String = "Tests.kt"
}

class FakeGradleCourseProjectGenerator(
  builder: FakeGradleCourseBuilder,
  course: Course
) : CourseProjectGenerator<EmptyProjectSettings>(builder, course) {
  override fun afterProjectGenerated(
    project: Project,
    projectSettings: EmptyProjectSettings,
    openCourseParams: Map<String, String>,
    onConfigurationFinished: () -> Unit
  ) {
    onConfigurationFinished()
  }

  override fun autoCreatedAdditionalFiles(holder: CourseInfoHolder<Course>): List<EduFile> {
    val existingAdditionalFiles = holder.course.additionalFiles.map { it.name }
    return listOf(BUILD_GRADLE, SETTINGS_GRADLE)
      .filter { it !in existingAdditionalFiles }
      .map { fileName ->
        EduFile(fileName, "")
      }
  }
}
