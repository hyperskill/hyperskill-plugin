package org.hyperskill.academy.python.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.coursecreator.archive.FileAttributesTest
import org.hyperskill.academy.coursecreator.archive.FileAttributesTest.Companion.expected
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.python.learning.PyConfigurator
import org.hyperskill.academy.python.learning.PyNewConfigurator
import org.junit.runners.Parameterized.Parameters

private fun pyData(): Collection<Array<Any>> {
  val expectedAttributes = expected(
    excludedFromArchive = true,
    archiveInclusionPolicy = ArchiveInclusionPolicy.MUST_EXCLUDE
  )

  return FileAttributesTest.data() + listOf(
    arrayOf("file.pyc", expectedAttributes),
    arrayOf("subfolder/file.pyc", expectedAttributes),

    arrayOf("__pycache__/", expectedAttributes),
    arrayOf("__pycache__/subfile", expectedAttributes),
    arrayOf("venv/", expectedAttributes),
    arrayOf("venv/subfile", expectedAttributes),
  )
}

class PyFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = PyConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = pyData()
  }
}

class PyNewFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = PyNewConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = pyData()
  }
}