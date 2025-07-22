package org.hyperskill.academy.scala.sbt

import com.intellij.openapi.project.Project
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.jvm.JdkProjectSettings
import org.hyperskill.academy.jvm.jvmEnvironmentSettings
import org.hyperskill.academy.jvm.stepik.fileName
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import org.hyperskill.academy.scala.isScalaPluginCompatible
import org.hyperskill.academy.scala.sbt.ScalaSbtCourseBuilder.Companion.BUILD_SBT
import org.hyperskill.academy.scala.sbt.checker.ScalaSbtTaskCheckerProvider
import org.jetbrains.plugins.scala.ScalaLanguage
import org.jetbrains.sbt.Sbt
import javax.swing.Icon

class ScalaSbtConfigurator : EduConfigurator<JdkProjectSettings> {
  override val courseBuilder: EduCourseBuilder<JdkProjectSettings>
    get() = ScalaSbtCourseBuilder()

  override val testFileName: String
    get() = TEST_SCALA

  override val isEnabled: Boolean
    get() = !EduUtilsKt.isAndroidStudio() && isScalaPluginCompatible

  override val taskCheckerProvider: TaskCheckerProvider
    get() = ScalaSbtTaskCheckerProvider()

  override fun getMockFileName(course: Course, text: String): String = fileName(ScalaLanguage.INSTANCE, text)

  override val mockTemplate: String
    get() = getInternalTemplateText(MOCK_SCALA)

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Scala

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    dirAndChildren("target") {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file(BUILD_SBT) {
      archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
    }

    dir(Sbt.ProjectDirectory()) {
      file(Sbt.PropertiesFile(), direct = true) {
        archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
      }
    }
  }

  companion object {
    const val TEST_SCALA = "TestSpec.scala"
    const val TASK_SCALA = "Task.scala"
    const val MAIN_SCALA = "Main.scala"
    const val MOCK_SCALA = "Mock.scala"
  }

  override fun getEnvironmentSettings(project: Project): Map<String, String> = jvmEnvironmentSettings(project)
}
