package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.openapi.extensions.PluginId
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.newproject.EduProjectSettings
import org.hyperskill.academy.learning.stepik.hyperskill.checker.HyperskillTaskCheckerProvider
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillCourseBuilder
import javax.swing.Icon

/**
 * Hyperskill contractors edit existing Hyperskill projects as Stepik lessons.
 * These lessons don't have language/environment inside, so we need to detect them.
 *
 */
abstract class HyperskillConfigurator<T : EduProjectSettings>(private val baseConfigurator: EduConfigurator<T>) : EduConfigurator<T> {

  override val taskCheckerProvider: TaskCheckerProvider
    get() = HyperskillTaskCheckerProvider(baseConfigurator.taskCheckerProvider)

  override val courseBuilder: EduCourseBuilder<T>
    get() = HyperskillCourseBuilder(baseConfigurator.courseBuilder)

  /**
   * We have to do this stuff because implementation by delegation still works unstable
   */
  override val testFileName: String
    get() = baseConfigurator.testFileName

  override val sourceDir: String
    get() = baseConfigurator.sourceDir

  override val testDirs: List<String>
    get() = baseConfigurator.testDirs

  override val isEnabled: Boolean
    get() = baseConfigurator.isEnabled

  override val isCourseCreatorEnabled: Boolean
    get() = baseConfigurator.isCourseCreatorEnabled

  override val mockTemplate: String
    get() = baseConfigurator.mockTemplate

  override val pluginRequirements: List<PluginId>
    get() = baseConfigurator.pluginRequirements

  override val logo: Icon
    get() = baseConfigurator.logo

  override val courseFileAttributesEvaluator: AttributesEvaluator =
    baseConfigurator.courseFileAttributesEvaluator

  override fun isTestFile(task: Task, path: String): Boolean {
    val isTestFile = baseConfigurator.isTestFile(task, path)
    val taskFile = task.getTaskFile(path)
    return isTestFile || taskFile?.isVisible == false
  }

  override fun getMockFileName(course: Course, text: String): String? = baseConfigurator.getMockFileName(course, text)

  override fun beforeCourseStarted(course: Course) {
    baseConfigurator.beforeCourseStarted(course)
  }

  companion object {
    const val HYPERSKILL_TEST_DIR = "hstest"
  }
}
