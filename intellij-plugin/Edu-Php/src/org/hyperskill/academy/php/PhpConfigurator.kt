package org.hyperskill.academy.php

import com.intellij.openapi.extensions.PluginId
import com.jetbrains.php.composer.ComposerUtils
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import javax.swing.Icon

class PhpConfigurator : EduConfigurator<PhpProjectSettings> {

  override val courseBuilder: EduCourseBuilder<PhpProjectSettings>
    get() = PhpCourseBuilder()

  override val testFileName: String
    get() = TEST_PHP

  override val taskCheckerProvider: TaskCheckerProvider
    get() = PhpTaskCheckerProvider()

  override val logo: Icon
    // the default icon from plugin looks ugly, so we use ours
    get() = EducationalCoreIcons.Language.Php

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val sourceDir: String
    get() = EduNames.SRC

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("com.jetbrains.php"))

  override fun getMockFileName(course: Course, text: String): String = TASK_PHP

  override fun isTestFile(task: Task, path: String): Boolean = super.isTestFile(task, path) || path == testFileName

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    dirAndChildren(ComposerUtils.VENDOR_DIR_DEFAULT_NAME) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file(ComposerUtils.CONFIG_DEFAULT_FILENAME) {
      archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
    }

    file(ComposerUtils.COMPOSER_PHAR_NAME) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
  }

  companion object {
    const val MAIN_PHP = "main.php"
    const val TASK_PHP = "task.php"
    const val TEST_PHP = "test.php"
  }
}