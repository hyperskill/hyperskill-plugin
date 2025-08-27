package org.hyperskill.academy.cpp.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.coursecreator.archive.FileAttributesTest
import org.hyperskill.academy.cpp.CppConfigurator
import org.junit.runners.Parameterized.Parameters

class CppFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {

  override val configurator = CppConfigurator()

  companion object {
    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = FileAttributesTest.data() + listOf(
      arrayOf("cmake-build-123/", expected(excludedFromArchive = true)),
      arrayOf("cmake-build-123/subfile", expected(excludedFromArchive = true)),

      arrayOf("test-framework/", expected(excludedFromArchive = true)),
      arrayOf("test-framework/subfile", expected(excludedFromArchive = true)),

      // in subfolders
      arrayOf("subfolder/cmake-build-123/", expected(excludedFromArchive = false)),
      arrayOf("subfolder/test-framework/", expected(excludedFromArchive = false))
    )
  }

}