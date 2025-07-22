package org.hyperskill.academy.php.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.coursecreator.archive.FileAttributesTest
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.php.PhpConfigurator
import org.junit.runners.Parameterized.Parameters

class PhpFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = PhpConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val expectedAttributes = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
      )

      return FileAttributesTest.data() + listOf(
        arrayOf("vendor/", expectedAttributes),
        arrayOf("subfolder/vendor/", expectedAttributes),
        arrayOf("subfolder/vendor/subfile", expectedAttributes),

        arrayOf("composer.phar", expectedAttributes),
        arrayOf("subfolder/composer.phar", expectedAttributes),
      )
    }
  }
}