package org.hyperskill.academy.learning.stepik.hyperskill.settings

import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.components.JBCheckBox
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.hyperskill.logInFakeHyperskillUser
import org.hyperskill.academy.learning.stepik.hyperskill.logOutFakeHyperskillUser
import org.junit.Test
import javax.swing.JComponent

class HyperskillOptionsTest : EduTestCase() {
  private lateinit var options: HyperskillOptions
  private lateinit var component: JComponent

  private val loginLinkText: String
    get() = UIUtil.findComponentOfType(component, HoverHyperlinkLabel::class.java)!!.text

  private val logInText: String
    get() = EduCoreBundle.message("log.in.to", options.displayName)

  private val logOutText: String
    get() = EduCoreBundle.message("log.out")

  override fun setUp() {
    super.setUp()
    logOutFakeHyperskillUser()
  }

  override fun tearDown() {
    try {
      logOutFakeHyperskillUser()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }

  // Since 2026.2 the non-modal Settings window calls `reset()` on a page that was unmodified when the window lost focus
  // and is modified when it regains focus. Logging in happens in the browser, so it spans exactly that focus change.
  @Test
  fun `test login from the page does not mark it as modified`() {
    openPage()

    logInFromPage()

    assertFalse(options.isModified)
  }

  @Test
  fun `test reset keeps the account after login from the page`() {
    openPage()
    logInFromPage()

    options.reset()

    assertNotNull(HyperskillSettings.INSTANCE.account)
    assertEquals(logOutText, loginLinkText)
  }

  @Test
  fun `test reset does not bring back the account after logout`() {
    logInFakeHyperskillUser()
    openPage()
    logOutFakeHyperskillUser()
    assertFalse(options.isModified)

    options.reset()

    assertNull(HyperskillSettings.INSTANCE.account)
    assertEquals(logInText, loginLinkText)
  }

  @Test
  fun `test apply keeps the account after login from another place`() {
    openPage()
    logInFakeHyperskillUser()

    options.apply()

    assertNotNull(HyperskillSettings.INSTANCE.account)
  }

  @Test
  fun `test reset shows the account after login from another place`() {
    openPage()
    logInFakeHyperskillUser()

    options.reset()

    assertEquals(logOutText, loginLinkText)
  }

  @Test
  fun `test auto update checkbox is still a regular setting`() {
    openPage()
    val checkBox = UIUtil.findComponentOfType(component, JBCheckBox::class.java)!!
    assertFalse(options.isModified)

    checkBox.isSelected = !checkBox.isSelected
    assertTrue(options.isModified)

    options.reset()
    assertFalse(options.isModified)
  }

  private fun openPage() {
    options = HyperskillOptions()
    component = options.createComponent()!!
    // The platform resets a page right after creating its component
    options.reset()
  }

  /** The login link passes [HyperskillOptions.postLoginActions] to `doAuthorize`, and they run once the account is set. */
  private fun logInFromPage() {
    logInFakeHyperskillUser()
    options.postLoginActions()
  }
}
