package org.hyperskill.academy.learning.settings

import com.intellij.ui.BrowserHyperlinkListener
import com.intellij.ui.HoverHyperlinkLabel
import com.intellij.ui.HyperlinkAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.uiDesigner.core.GridConstraints
import com.intellij.uiDesigner.core.GridLayoutManager
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.learning.RemoteEnvHelper
import org.hyperskill.academy.learning.authUtils.Account
import org.hyperskill.academy.learning.courseFormat.UserInfo
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.ui.createTextPane
import javax.swing.JComponent
import javax.swing.JPanel

// Implement this class to show oauth settings with login/logout link
abstract class LoginOptions<T : Account<out UserInfo>> : OptionsProvider {
  private var browseProfileLabel = createTextPane()
  private var loginLink = HoverHyperlinkLabel("")
  private var accountPanel = JPanel()
  private var mainPanel: JPanel = JPanel()
  private var loginListener: HyperlinkAdapter? = null

  abstract fun getCurrentAccount(): T?

  protected abstract fun profileUrl(account: T): String

  protected abstract fun createAuthorizeListener(): HyperlinkAdapter

  protected abstract fun createLogOutListener(): HyperlinkAdapter?

  override fun isAvailable(): Boolean = !RemoteEnvHelper.isRemoteDevServer()

  private fun initUI() {
    val additionalComponents = getAdditionalComponents()
    mainPanel = JPanel(GridLayoutManager(additionalComponents.size + 1, 1))
    mainPanel.border = IdeBorderFactory.createTitledBorder(displayName)

    accountPanel = JPanel(GridLayoutManager(1, 2))
    addBrowseProfileLabel()
    addLoginLink()

    fun gridConstraints(row: Int): GridConstraints {
      return GridConstraints().apply {
        this.row = row
        column = 0
        anchor = GridConstraints.ANCHOR_WEST
      }
    }

    mainPanel.add(accountPanel, gridConstraints(0))
    additionalComponents.forEachIndexed { index, component -> mainPanel.add(component, gridConstraints(index + 1)) }
  }

  protected open fun getAdditionalComponents(): List<JComponent> = emptyList()

  private fun addBrowseProfileLabel() {
    browseProfileLabel = createTextPane()
    browseProfileLabel.background = UIUtil.getPanelBackground()
    val constraints = GridConstraints()
    constraints.row = 0
    constraints.column = 0
    constraints.anchor = GridConstraints.ANCHOR_WEST
    constraints.hSizePolicy = GridConstraints.SIZEPOLICY_FIXED
    browseProfileLabel.addHyperlinkListener(BrowserHyperlinkListener.INSTANCE)
    accountPanel.add(browseProfileLabel, constraints)
  }

  private fun addLoginLink() {
    loginLink = HoverHyperlinkLabel("")
    val constraints = GridConstraints()
    constraints.row = 0
    constraints.column = 1
    constraints.anchor = GridConstraints.ANCHOR_WEST
    accountPanel.add(loginLink, constraints)
  }

  override fun createComponent(): JComponent? {
    initUI()
    return mainPanel
  }

  // Logging in or out takes effect immediately, so the account is never a pending edit of this page.
  // Since 2026.2 the non-modal Settings window calls `reset` on a page that became modified while the window was unfocused,
  // and logging in happens in the browser, so comparing accounts here would undo every login made from this page.
  override fun isModified(): Boolean = false

  override fun reset() {
    updateLoginLabels()
  }

  override fun apply() {}

  protected fun updateLoginLabels() {
    if (loginListener != null) {
      loginLink.removeHyperlinkListener(loginListener)
    }

    val selectedAccount = getCurrentAccount()

    val userInfo = selectedAccount?.userInfo
    if (selectedAccount == null || userInfo == null) {
      browseProfileLabel.text = EduCoreBundle.message("not.logged.in")
      loginLink.text = EduCoreBundle.message("log.in.to", displayName)
      loginListener = createAuthorizeListener()
    }
    else {
      browseProfileLabel.text = EduCoreBundle.message("logged.in.as.verbose", "<a href=${profileUrl(selectedAccount)}>${userInfo}</a>")
      loginLink.text = getLogoutText()
      loginListener = createLogOutListener()
    }

    loginLink.addHyperlinkListener(loginListener)
  }

  open fun getLogoutText(): String = EduCoreBundle.message("log.out")
}
