package org.hyperskill.academy.php

import com.jetbrains.php.composer.ComposerUtils
import com.jetbrains.php.config.interpreters.PhpInterpreter
import com.jetbrains.php.config.interpreters.PhpInterpretersManagerImpl
import com.jetbrains.php.lang.PhpLanguage
import org.hyperskill.academy.learning.course
import org.hyperskill.academy.learning.courseGeneration.CourseGenerationTestBase
import org.hyperskill.academy.learning.fileTree
import org.hyperskill.academy.php.PhpConfigurator.Companion.TASK_PHP
import org.hyperskill.academy.php.PhpConfigurator.Companion.TEST_PHP
import org.junit.Test

class PhpCourseBuilderTest : CourseGenerationTestBase<PhpProjectSettings>() {
  override val defaultSettings = PhpProjectSettings()
  override fun setUp() {
    super.setUp()
    val phpInterpreter = PhpInterpreter()
    phpInterpreter.name = "interpreterName"
    defaultSettings.phpInterpreter = phpInterpreter
    PhpInterpretersManagerImpl.getInstance(project).addInterpreter(phpInterpreter)
  }

  @Test
  fun `test study course structure`() {
    val changedConfig = "{" +
                        "  \"require\": {" +
                        "    \"phpunit/phpunit\": \"^9\"" +
                        "  }" +
                        "}"
    val course = course(language = PhpLanguage.INSTANCE) {
      lesson("lesson1") {
        eduTask("task1") {
          dir("src") {
            taskFile(TASK_PHP)
          }
          dir("test") {
            taskFile(TEST_PHP)
          }
        }
      }
      additionalFiles {
        eduFile(ComposerUtils.CONFIG_DEFAULT_FILENAME, changedConfig)
      }
    }
    createCourseStructure(course)

    fileTree {
      dir("lesson1/task1") {
        dir("src") {
          file(TASK_PHP)
        }
        dir("test") {
          file(TEST_PHP)
        }
        file("task.html")
      }
      file(ComposerUtils.CONFIG_DEFAULT_FILENAME)
    }.assertEquals(rootDir)

    // student gets config changed by a teacher
    assertListOfAdditionalFiles(
      course,
      ComposerUtils.CONFIG_DEFAULT_FILENAME to changedConfig
    )
  }
}