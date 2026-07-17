package org.hyperskill.academy.learning

import org.junit.Test

class EduSettingsTest : EduSettingsServiceTestBase() {

  @Test
  fun `test serialization with JCEF`() {
    val settings = EduSettings(jcefSupportedOverride = true)
    settings.checkState(
      """
      <EduSettings>
        <option name="javaUiLibrary" value="JCEF" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """
    )
  }

  @Test
  fun `test serialization with Swing`() {
    val settings = EduSettings(jcefSupportedOverride = false)
    settings.checkState(
      """
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """
    )
  }

  @Test
  fun `test set Swing explicitly settings`() {
    val settings = EduSettings(jcefSupportedOverride = true)
    settings.setJavaUiLibrary(JavaUILibrary.SWING, true)
    settings.checkState(
      """
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="true" />
      </EduSettings>
    """
    )
  }

  @Test
  fun `test switch to JCEF from Swing`() {
    val settings = EduSettings(jcefSupportedOverride = true)
    settings.loadStateAndCheck(
      """
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """, """
      <EduSettings>
        <option name="javaUiLibrary" value="JCEF" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """
    )
  }

  @Test
  fun `test switch to Swing from JCEF`() {
    val settings = EduSettings(jcefSupportedOverride = false)
    settings.loadStateAndCheck(
      """
      <EduSettings>
        <option name="javaUiLibrary" value="JCEF" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """, """
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="false" />
      </EduSettings>
    """
    )
  }

  @Test
  fun `test preserve user choice`() {
    val settings = EduSettings(jcefSupportedOverride = true)
    settings.loadStateAndCheck(
      """
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="true" />
      </EduSettings>
    """, """
      <EduSettings>
        <option name="javaUiLibrary" value="Swing" />
        <option name="uiLibraryChangedByUser" value="true" />
      </EduSettings>
    """
    )
  }

}
