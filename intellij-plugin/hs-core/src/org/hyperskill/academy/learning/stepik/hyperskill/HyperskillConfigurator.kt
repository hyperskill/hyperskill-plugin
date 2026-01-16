package org.hyperskill.academy.learning.stepik.hyperskill

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.vfs.VfsUtilCore
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

  override val mockTemplate: String
    get() = baseConfigurator.mockTemplate

  override val pluginRequirements: List<PluginId>
    get() = baseConfigurator.pluginRequirements

  override val logo: Icon
    get() = baseConfigurator.logo

  override val courseFileAttributesEvaluator: AttributesEvaluator =
    baseConfigurator.courseFileAttributesEvaluator

  override fun isTestFile(task: Task, path: String): Boolean {
    // Use this.testDirs instead of delegating to baseConfigurator to respect overridden testDirs
    val isTestFile = path == testFileName || testDirs.any { testDir -> VfsUtilCore.isEqualOrAncestor(testDir, path) }
    // ALT-10961: Also check for legacy Hyperskill test files that should be hidden
    val isLegacyTestFile = path in LEGACY_HYPERSKILL_TEST_FILES
    val taskFile = task.getTaskFile(path)
    return isTestFile || isLegacyTestFile || taskFile?.isVisible == false
  }

  override fun getMockFileName(course: Course, text: String): String? = baseConfigurator.getMockFileName(course, text)

  override fun beforeCourseStarted(course: Course) {
    baseConfigurator.beforeCourseStarted(course)
  }

  companion object {
    const val HYPERSKILL_TEST_DIR = "hstest"

    /**
     * Legacy test file names used in older Hyperskill projects.
     * These files should be hidden in Project View but are not used for running tests
     * when a proper test directory (e.g., `test/`) exists.
     * Modern projects use `test/tests.py` inside the test directory instead.
     */
    private val LEGACY_HYPERSKILL_TEST_FILES = setOf("tests.py")
  }
}
