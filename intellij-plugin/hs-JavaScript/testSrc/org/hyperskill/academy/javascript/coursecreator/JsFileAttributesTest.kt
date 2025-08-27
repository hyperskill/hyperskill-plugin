package org.hyperskill.academy.javascript.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.coursecreator.archive.FileAttributesTest
import org.hyperskill.academy.javascript.learning.JsConfigurator
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.junit.runners.Parameterized.Parameters

class JsFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {

  override val configurator: EduConfigurator<*> = JsConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {

      val expectedAttributesForNodeModules = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
      )
      val expectedAttributesForPackageLock = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.AUTHOR_DECISION
      )

      return FileAttributesTest.data() + listOf(
        arrayOf("node_modules/", expectedAttributesForNodeModules),
        arrayOf("subfolder/node_modules/", expectedAttributesForNodeModules),
        arrayOf("subfolder/node_modules/subfile", expectedAttributesForNodeModules),

        arrayOf("package-lock.json", expectedAttributesForPackageLock),
        arrayOf("subfolder/package-lock.json", expectedAttributesForPackageLock)
      )
    }
  }
}