package org.hyperskill.academy.kotlin.coursecreator

import org.hyperskill.academy.coursecreator.archive.ExpectedCourseFileAttributes
import org.hyperskill.academy.jvm.coursecreator.GradleFileAttributesTest
import org.hyperskill.academy.kotlin.KtConfigurator
import org.hyperskill.academy.learning.configuration.EduConfigurator
import org.junit.runners.Parameterized.Parameters

class KotlinFileAttributesTest(
  filePath: String,
  expectedAttributes: ExpectedCourseFileAttributes
) : GradleFileAttributesTest(filePath, expectedAttributes) {

  override val configurator: EduConfigurator<*> = KtConfigurator()

  companion object {

    @JvmStatic
    @Parameters(name = "{0}")
    fun data(): Collection<Array<Any>> = GradleFileAttributesTest.data() // there are no special kotlin rules
  }
}