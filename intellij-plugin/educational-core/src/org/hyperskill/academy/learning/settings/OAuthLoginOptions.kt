package org.hyperskill.academy.learning.settings

import com.intellij.ui.HyperlinkAdapter
import org.hyperskill.academy.learning.api.EduOAuthCodeFlowConnector
import org.hyperskill.academy.learning.authUtils.AuthorizationPlace
import org.hyperskill.academy.learning.authUtils.EduLoginConnector
import org.hyperskill.academy.learning.authUtils.OAuthAccount
import org.hyperskill.academy.learning.courseFormat.UserInfo
import javax.swing.event.HyperlinkEvent

abstract class OAuthLoginOptions<T : OAuthAccount<out UserInfo>> : LoginOptions<T>() {
  protected abstract val connector: EduLoginConnector<T, *>

  override fun getCurrentAccount(): T? = connector.account

  override fun setCurrentAccount(account: T?) {
    connector.account = account
  }

  override fun createAuthorizeListener(): HyperlinkAdapter =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(e: HyperlinkEvent) {
        connector.doAuthorize({ postLoginActions() }, authorizationPlace = AuthorizationPlace.SETTINGS)
      }
    }

  open fun postLoginActions() {
    lastSavedAccount = getCurrentAccount()
    updateLoginLabels()
  }

  override fun createLogOutListener(): HyperlinkAdapter? =
    object : HyperlinkAdapter() {
      override fun hyperlinkActivated(event: HyperlinkEvent) {
        val currentConnector = connector
        if (currentConnector is EduOAuthCodeFlowConnector) {
          lastSavedAccount = null
          currentConnector.doLogout(authorizationPlace = AuthorizationPlace.SETTINGS)
          updateLoginLabels()
        }
      }
    }
}