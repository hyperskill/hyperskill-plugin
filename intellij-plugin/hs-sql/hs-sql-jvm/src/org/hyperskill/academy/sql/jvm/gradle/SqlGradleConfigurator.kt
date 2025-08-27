package org.hyperskill.academy.sql.jvm.gradle

import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.jvm.gradle.GradleConfiguratorBase
import org.hyperskill.academy.jvm.gradle.GradleCourseBuilderBase
import org.hyperskill.academy.jvm.gradle.checker.GradleTaskCheckerProvider
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.sql.core.SqlConfiguratorBase

class SqlGradleConfigurator : GradleConfiguratorBase(), SqlConfiguratorBase<JdkProjectSettings> {

  // A proper test template is provided via `SqlGradleCourseBuilder.testTemplateName`
  override val testFileName: String = ""

  override val courseBuilder: GradleCourseBuilderBase
    get() = SqlGradleCourseBuilder()

  override val courseFileAttributesEvaluator: AttributesEvaluator =
    AttributesEvaluator(super<GradleConfiguratorBase>.courseFileAttributesEvaluator) {
      extension(DB_EXTENSION) {
        excludeFromArchive()
        archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
      }
    }

  override val taskCheckerProvider: TaskCheckerProvider
    get() = GradleTaskCheckerProvider()

  companion object {
    private const val DB_EXTENSION = "db"
  }
}
