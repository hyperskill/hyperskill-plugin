package org.hyperskill.academy.coursecreator.framework.impl

import org.hyperskill.academy.coursecreator.framework.CCFrameworkLessonManager
import org.hyperskill.academy.learning.EduSettingsServiceTestBase
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
