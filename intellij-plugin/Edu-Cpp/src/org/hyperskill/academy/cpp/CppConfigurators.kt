package org.hyperskill.academy.cpp

import com.jetbrains.cmake.CMakeListsFileType
import org.hyperskill.academy.EducationalCoreIcons
import org.hyperskill.academy.cpp.CMakeConstants.CMAKE_CATCH
import org.hyperskill.academy.cpp.CMakeConstants.CMAKE_DIRECTORY
import org.hyperskill.academy.cpp.CMakeConstants.CMAKE_GOOGLE_TEST
import org.hyperskill.academy.cpp.CMakeConstants.CMAKE_GOOGLE_TEST_DOWNLOAD
import org.hyperskill.academy.cpp.CMakeConstants.CMAKE_UTILS
import org.hyperskill.academy.cpp.checker.CppCatchTaskCheckerProvider
import org.hyperskill.academy.cpp.checker.CppGTaskCheckerProvider
import org.hyperskill.academy.cpp.checker.CppTaskCheckerProvider
import org.hyperskill.academy.learning.EduCourseBuilder
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.checker.TaskCheckerProvider
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.learning.configuration.attributesEvaluator.AttributesEvaluator
import org.hyperskill.academy.learning.courseFormat.Course
import org.hyperskill.academy.learning.courseGeneration.GeneratorUtils.getInternalTemplateText
import javax.swing.Icon

class CppGTestConfigurator : CppConfigurator() {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppGTestCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CppGTaskCheckerProvider()
}

class CppCatchConfigurator : CppConfigurator() {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppCatchCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CppCatchTaskCheckerProvider()
}

open class CppConfigurator : EduConfigurator<CppProjectSettings> {
  override val courseBuilder: EduCourseBuilder<CppProjectSettings>
    get() = CppCourseBuilder()

  override val taskCheckerProvider: TaskCheckerProvider
    get() = CppTaskCheckerProvider()

  override val testFileName: String
    get() = TEST_CPP

  override fun getMockFileName(course: Course, text: String): String = MAIN_CPP

  override val sourceDir: String
    get() = EduNames.SRC

  override val testDirs: List<String>
    get() = listOf(EduNames.TEST)

  override val mockTemplate: String
    get() = getInternalTemplateText(MAIN_CPP)

  override val isCourseCreatorEnabled: Boolean
    get() = true

  override val logo: Icon
    get() = EducationalCoreIcons.Language.Cpp

  override val courseFileAttributesEvaluator: AttributesEvaluator = AttributesEvaluator(super.courseFileAttributesEvaluator) {
    // we could use it how indicator because CLion generate build dirs with names `cmake-build-*`
    // @see com.jetbrains.cidr.cpp.cmake.workspace.CMakeWorkspace.getProfileGenerationDirNames
    dirAndChildren("^cmake-build-".toRegex(), TEST_FRAMEWORKS_BASE_DIR_VALUE, direct = true) {
      excludeFromArchive()
      archiveInclusionPolicy(ArchiveInclusionPolicy.MUST_EXCLUDE)
    }

    file(CMakeListsFileType.FILE_NAME, direct = true) {
      archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
    }

    dir(CMAKE_DIRECTORY, direct = true) {
      file(
        CMAKE_UTILS,
        CMAKE_GOOGLE_TEST,
        CMAKE_GOOGLE_TEST_DOWNLOAD,
        CMAKE_CATCH,
        direct = true
      ) {
        archiveInclusionPolicy(ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
      }
    }
  }

  override val defaultPlaceholderText: String
    get() = "/* TODO */"

  companion object {
    const val MAIN_CPP = "main.cpp"
    const val TASK_CPP = "task.cpp"
    const val TEST_CPP = "test.cpp"
  }
}