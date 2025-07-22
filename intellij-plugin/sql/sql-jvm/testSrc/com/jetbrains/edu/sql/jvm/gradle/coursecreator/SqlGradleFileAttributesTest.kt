package org.hyperskill.academy.sql.jvm.gradle.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.coursecreator.archive.FileAttributesTest
import org.hyperskill.academy.jvm.coursecreator.GradleFileAttributesTest
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.sql.jvm.gradle.SqlGradleConfigurator
import org.junit.runners.Parameterized.Parameters

class SqlGradleFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = SqlGradleConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val expectedAttributes = expected(
        excludedFromArchive = true,
        archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
      )

      return GradleFileAttributesTest.data() + listOf(
        arrayOf("file.db", expectedAttributes),
        arrayOf("subfolder/file.db", expectedAttributes),
      )
    }
  }
}