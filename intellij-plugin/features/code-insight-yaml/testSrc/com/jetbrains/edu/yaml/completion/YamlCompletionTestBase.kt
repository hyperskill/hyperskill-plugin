package com.jetbrains.edu.yaml.completion

import com.jetbrains.edu.codeInsight.EduCompletionTextFixture
import com.jetbrains.edu.yaml.YamlCodeInsightTest

abstract class YamlCompletionTestBase : YamlCodeInsightTest() {

  private lateinit var completionFixture: EduCompletionTextFixture

  override fun setUp() {
    super.setUp()
    completionFixture = EduCompletionTextFixture(myFixture)
    completionFixture.setUp()
  }

  override fun tearDown() {
    try {
      completionFixture.tearDown()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

}
