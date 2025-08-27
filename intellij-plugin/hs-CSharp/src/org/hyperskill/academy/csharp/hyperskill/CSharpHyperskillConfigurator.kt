package org.hyperskill.academy.csharp.hyperskill

import com.intellij.openapi.vfs.VirtualFile
import org.hyperskill.academy.csharp.CSharpConfigurator
import org.hyperskill.academy.csharp.CSharpProjectSettings
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.stepik.hyperskill.HyperskillConfigurator

/**
 * For now, we assume that Hyperskill courses can only be Unity-based
 */
class CSharpHyperskillConfigurator : HyperskillConfigurator<CSharpProjectSettings>(CSharpConfigurator()) {
  override val courseBuilder: EduCourseBuilder<CSharpProjectSettings>
    get() = CSharpHyperskillCourseBuilder()

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    extension("meta") {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }
  }

  override fun shouldFileBeVisibleToStudent(virtualFile: VirtualFile): Boolean =
    virtualFile.name == "Packages" || virtualFile.name == "ProjectSettings" || virtualFile.path.contains("/Assets/")
}
