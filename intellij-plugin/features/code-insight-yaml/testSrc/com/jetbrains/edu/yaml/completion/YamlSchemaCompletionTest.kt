package com.jetbrains.edu.yaml.completion

import org.junit.Test

class YamlSchemaCompletionTest : YamlCompletionTestBase() {

  @Test
  fun `test no completion in non-config file`() {
    myFixture.configureByText(
      "random.yaml", """
      |title: Test Course
      |type: coursera
      |language: <caret>Russian
      |summary: sum
      |programming_language: Plain text
      |programming_language_version: 1.42
      |environment: Android
      |content:
      |- lesson1
      |
    """.trimMargin("|")
    )

    val lookupElements = myFixture.completeBasic()
    assertEmpty(lookupElements)
  }
}
