package org.hyperskill.academy.scala.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.jvm.coursecreator.GradleFileAttributesTest
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.hyperskill.academy.scala.gradle.ScalaGradleConfigurator
import org.junit.runners.Parameterized.Parameters

class ScalaFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : GradleFileAttributesTest(filePath, expectedAttributes) {

  override val configurator: EduConfigurator<*> = ScalaGradleConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = GradleFileAttributesTest.data() // there are no special scala rules
  }
}