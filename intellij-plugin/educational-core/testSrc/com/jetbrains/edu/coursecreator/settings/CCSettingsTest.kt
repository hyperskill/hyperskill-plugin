package org.hyperskill.academy.coursecreator.settings

import org.hyperskill.academy.learning.EduSettingsServiceTestBase
import org.junit.Test

class CCSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization`() {
    val settings = CCSettings()
    settings.loadStateAndCheck(
      """
      <State>
        <option name="copyTestsInFrameworkLessons" value="true" />
        <option name="isHtmlDefault" value="true" />
        <option name="showSplitEditor" value="true" />
      </State>
    """
    )
  }
}
