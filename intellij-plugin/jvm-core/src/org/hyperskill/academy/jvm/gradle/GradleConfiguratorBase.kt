package org.hyperskill.academy.jvm.gradle

import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.project.Project
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.jvm.jvmEnvironmentSettings
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.gradle.GradleConstants.GRADLE
import org.hyperskill.academy.learning.gradle.GradleConstants.GRADLE_PROPERTIES
import org.hyperskill.academy.learning.gradle.GradleConstants.GRADLE_WRAPPER_JAR
import org.hyperskill.academy.learning.gradle.GradleConstants.GRADLE_WRAPPER_PROPERTIES
import org.hyperskill.academy.learning.gradle.GradleConstants.GRADLE_WRAPPER_UNIX
import org.hyperskill.academy.learning.gradle.GradleConstants.GRADLE_WRAPPER_WIN
import org.hyperskill.academy.learning.gradle.GradleConstants.LOCAL_PROPERTIES
import org.hyperskill.academy.learning.gradle.GradleConstants.SETTINGS_GRADLE
import org.jetbrains.plugins.gradle.util.GradleConstants

abstract class GradleConfiguratorBase : EduConfigurator<JdkProjectSettings> {
  abstract override val courseBuilder: GradleCourseBuilderBase

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    file(SETTINGS_GRADLE, GRADLE_PROPERTIES, GradleConstants.KOTLIN_DSL_SCRIPT_NAME, GradleConstants.KOTLIN_DSL_SETTINGS_FILE_NAME) {
      includeIntoArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
    }

    dirAndChildren(*FOLDERS_TO_EXCLUDE) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file(*NAMES_TO_EXCLUDE) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
  }

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val pluginRequirements: List<PluginId>
    get() = listOf(PluginId.getId("org.jetbrains.plugins.gradle"), PluginId.getId("JUnit"))

  companion object {
    private val NAMES_TO_EXCLUDE = arrayOf(
      "EduTestRunner.java", GRADLE_WRAPPER_UNIX, GRADLE_WRAPPER_WIN, LOCAL_PROPERTIES,
      GRADLE_WRAPPER_JAR, GRADLE_WRAPPER_PROPERTIES
    )

    private val FOLDERS_TO_EXCLUDE = arrayOf(EduNames.OUT, EduNames.BUILD, GRADLE)
  }

  override fun getEnvironmentSettings(project: Project): Map<String, String> = jvmEnvironmentSettings(project)
}
