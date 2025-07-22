package org.hyperskill.academy.rust.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.coursecreator.archive.FileAttributesTest
import org.hyperskill.academy.learning.configuration.ArchiveInclusionPolicy
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.rust.RsConfigurator
import org.junit.runners.Parameterized.Parameters
import org.rust.cargo.CargoConstants

class RsFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : FileAttributesTest(filePath, expectedAttributes) {
  override val configurator: EduConfigurator<*> = RsConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> {
      val insideArchive = expected(
        excludedFromArchive = false,
        archiveInclusionPolicy = ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT
      )

      return FileAttributesTest.data() + listOf(
        arrayOf(".cargo/", insideArchive),
        arrayOf(".cargo/${CargoConstants.CONFIG_TOML_FILE}", insideArchive),
        arrayOf(".cargo/${CargoConstants.CONFIG_FILE}", insideArchive),

        arrayOf(
          ".cargo/other-file",
          expected(excludedFromArchive = true, archiveInclusionPolicy = ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
        ),
        arrayOf(
          "Cargo.toml",
          expected(excludedFromArchive = false, archiveInclusionPolicy = ArchiveInclusionPolicy.INCLUDED_BY_DEFAULT)
        ),
      )
    }
  }
}