package org.hyperskill.academy.rust

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.util.BuildNumber
import com.intellij.util.text.VersionComparatorUtil
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseFormat.PluginInfos
import org.hyperskill.academy.learning.pluginVersion
import org.hyperskill.academy.rust.checker.RsTaskCheckerProvider
import org.rust.cargo.CargoConstants
import org.rust.lang.RsConstants
import javax.swing.Icon

private val BUILD_242: BuildNumber = BuildNumber.fromString("242")!!

class RsConfigurator : EduConfigurator<RsProjectSettings> {
  override val taskCheckerProvider: RsTaskCheckerProvider
    get() = RsTaskCheckerProvider()

  override val testFileName: String
    get() = ""

  override fun getMockFileName(course: Course, text: String): String = RsConstants.MAIN_RS_FILE

  override val courseBuilder: EduCourseBuilder<RsProjectSettings>
    get() = RsCourseBuilder()

  override val testDirs: List<String>
    get() = listOf("tests")

  override val sourceDir: String
    get() = "src"

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Rust

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    dir(".cargo", direct = true) {
      name(CargoConstants.CONFIG_TOML_FILE, CargoConstants.CONFIG_FILE, direct = true) {
        includeIntoArchive()
        archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
      }
      includeIntoArchive()
    }

    dirAndChildren(CargoConstants.ProjectLayout.target) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file(CargoConstants.LOCK_FILE) {
      excludeFromArchive()
    }

    file("Cargo.toml") {
      archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
    }
  }

  override val isEnabled: Boolean
    get() {
      val rustPluginVersion = pluginVersion(PluginInfos.RUST.stringId) ?: return false
      val currentBuild = ApplicationInfo.getInstance().build
      // Rust plugin changed `RsToolchainPathChoosingComboBox` API since `241.27011.175`.
      // Also, since `242.23726` `org.rust.lang.core.psi.ext.containingCargoTarget` was changed as well.
      // Let's avoid runtime error because of this binary incompatibility
      val minSupportedVersion = if (currentBuild < BUILD_242) "241.27011.175" else "242.23726"
      return VersionComparatorUtil.compare(rustPluginVersion, minSupportedVersion) >= 0
    }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

}
