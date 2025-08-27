package org.hyperskill.academy.java.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.java.JConfigurator
import org.hyperskill.academy.jvm.coursecreator.GradleFileAttributesTest
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.junit.runners.Parameterized.Parameters

class JavaFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : GradleFileAttributesTest(filePath, expectedAttributes) {

  override val configurator: EduConfigurator<*> = JConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = GradleFileAttributesTest.data() // there are no special java rules
  }
}