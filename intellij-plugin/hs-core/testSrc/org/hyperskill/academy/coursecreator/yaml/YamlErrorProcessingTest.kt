package org.hyperskill.academy.coursecreator.yaml

import com.fasterxml.jackson.databind.exc.MismatchedInputException
import com.fasterxml.jackson.databind.exc.ValueInstantiationException
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.MarkedYAMLException
import com.fasterxml.jackson.module.kotlin.MissingKotlinParameterException
import com.intellij.lang.Language
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.LightVirtualFile
import org.hyperskill.academy.learning.yaml.YamlConfigSettings
import org.hyperskill.academy.learning.yaml.YamlDeserializer.deserializeCourse
import org.hyperskill.academy.learning.yaml.YamlLoader
import org.hyperskill.academy.learning.yaml.YamlMapper.basicMapper
import org.hyperskill.academy.learning.yaml.YamlTestCase
import org.hyperskill.academy.learning.yaml.deserializeItemProcessingErrors
import org.hyperskill.academy.learning.yaml.errorHandling.InvalidYamlFormatException
import org.junit.Test

class YamlErrorProcessingTest : YamlTestCase() {

  @Test
  fun `test empty field`() {
    doTest(
      """
            |title:
            |language: Russian
            |summary: |-
            |  This is a course about string theory.
            |  Why not?"
            |programming_language: Plain text
            |content:
            |- the first lesson
            |- the second lesson
            |""".trimMargin(), YamlConfigSettings.COURSE_CONFIG,
      "title is empty", MissingKotlinParameterException::class.java
    )
  }

  @Test
  fun `test unexpected symbol`() {
    @Suppress("DEPRECATION")
    doTest(
      """
            |title: Test course
            |language: Russian
            |summary: |-
            |  This is a course about string theory.
            |  Why not?"
            |programming_language: Plain text
            |content:e
            |- the first lesson
            |- the second lesson
            |""".trimMargin(), YamlConfigSettings.COURSE_CONFIG,
      "could not find expected ':' at line 7",
      MarkedYAMLException::class.java
    )
  }

  @Test
  fun `test parameter name without semicolon`() {
    doTest(
      """
            |title
            |language: Russian
            |summary: |-
            |  This is a course about string theory.
            |  Why not?"
            |programming_language: Plain text
            |content:
            |- the first lesson
            |""".trimMargin(), YamlConfigSettings.COURSE_CONFIG,
      "Invalid config", MismatchedInputException::class.java
    )
  }

  @Test
  fun `test unexpected item type`() {
    // Unsupported task types are now silently logged instead of throwing exceptions (see yamlDeserializationUtil.kt)
    // This test verifies that no exception is thrown for unsupported task types
    val configFile = createConfigFile(
      YamlConfigSettings.TASK_CONFIG,
      """
      |type: e
      |files:
      |- name: Test.java
      |  visible: true
      |is_multiple_choice: false
      |options:
      |- text: 1
      |  is_correct: true
      |- text: 2
      |  is_correct: false
      |""".trimMargin()
    )
    // Should return null without throwing an exception
    val result = deserializeItemProcessingErrors(configFile, project)
    assertNull("Unsupported task types should return null without throwing", result)
  }

  @Test
  fun `test task without type`() {
    doTest(
      """
    """.trimIndent(), YamlConfigSettings.TASK_CONFIG,
      "Task type is not specified", InvalidYamlFormatException::class.java
    )
  }

  @Test
  fun `test task file without name`() {
    doTest(
      """
    |type: edu
    |files:
    |- name:
    |  visible: true
    |""".trimMargin(), YamlConfigSettings.TASK_CONFIG,
      "File without a name is not allowed", InvalidYamlFormatException::class.java
    )
  }

  @Test
  fun `test language without configurator`() {
    val name = "Test Course"
    val language = "Russian"
    val programmingLanguage = "HTML"
    val firstLesson = "the first lesson"
    val secondLesson = "the second lesson"

    // check language is registered
    assertNotNull(Language.getRegisteredLanguages().find { it.displayName == programmingLanguage })

    // check exception as there's no configurator for this language
    assertThrows(ValueInstantiationException::class.java) {
      val yamlContent = """
      |title: $name
      |language: $language
      |summary: |-
      |  This is a course about string theory.
      |  Why not?"
      |programming_language: $programmingLanguage
      |content:
      |- $firstLesson
      |- $secondLesson
      |""".trimMargin()
      basicMapper().deserializeCourse(yamlContent)
    }
  }

  private fun <T : Exception> doTest(
    yamlContent: String,
    configName: String,
    expectedErrorMessage: String,
    expectedExceptionClass: Class<T>
  ) {
    try {
      val configFile = createConfigFile(configName, yamlContent)
      deserializeItemProcessingErrors(configFile, project)
    }
    catch (e: Exception) {
      assertInstanceOf(e, YamlLoader.ProcessedException::class.java)
      assertInstanceOf(e.cause, expectedExceptionClass)
      assertEquals(expectedErrorMessage, e.message)
      return
    }

    fail("Exception wasn't thrown")
  }

  private fun createConfigFile(configName: String, yamlContent: String): LightVirtualFile {
    val configFile = LightVirtualFile(configName)
    runWriteAction { VfsUtil.saveText(configFile, yamlContent) }
    return configFile
  }
}
