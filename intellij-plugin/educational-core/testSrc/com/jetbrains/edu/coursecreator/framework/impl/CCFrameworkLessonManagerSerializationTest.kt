package com.jetbrains.edu.coursecreator.framework.impl

import com.jetbrains.edu.coursecreator.framework.CCFrameworkLessonManager
import com.jetbrains.edu.learning.EduSettingsServiceTestBase
import org.junit.Test

class CCFrameworkLessonManagerSerializationTest : EduSettingsServiceTestBase() {
  @Test
  fun `test empty storage serialization`() {
    CCFrameworkLessonManager.getInstance(project).loadStateAndCheck(
      """
      <RecordState />
    """.trimIndent()
    )
  }
}
