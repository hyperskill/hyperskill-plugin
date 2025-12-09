package org.hyperskill.academy.learning.stepik.hyperskill.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.messages.Topic
import com.intellij.util.xmlb.XmlSerializer
import com.intellij.util.xmlb.annotations.Transient
import org.hyperskill.academy.learning.EduLogInListener
import org.hyperskill.academy.learning.authUtils.deserializeOAuthAccount
import org.hyperskill.academy.learning.authUtils.serializeAccount
import org.hyperskill.academy.learning.createTopic
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillAccount
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillUserInfo
import org.jdom.Element

private const val serviceName = "HsHyperskillSettings"
private val LOG = logger<HyperskillSettings>()

@State(name = serviceName, storages = [Storage("hyperskill.xml")])
class HyperskillSettings : PersistentStateComponent<Element> {
  @get:Transient
  @set:Transient
  @field:Volatile
  var account: HyperskillAccount? = null
    set(account) {
      val oldAccount = field
      field = account
      if (account != null) {
        val userInfo = account.userInfo
        LOG.info("Hyperskill account set: userId=${userInfo?.id ?: "unknown"}, userName=${userInfo?.getFullName() ?: "unknown"}")
      }
      else if (oldAccount != null) {
        LOG.info("Hyperskill account cleared (logged out)")
      }
      HyperskillConnector.getInstance().apply {
        if (account != null) {
          notifyUserLoggedIn()
          ApplicationManager.getApplication().messageBus.syncPublisher(LOGGED_IN_TO_HYPERSKILL).userLoggedIn()
        }
        else {
          notifyUserLoggedOut()
        }
      }
    }

  var updateAutomatically: Boolean = true

  override fun getState(): Element? {
    val mainElement = Element(serviceName)
    XmlSerializer.serializeInto(this, mainElement)
    val userElement = account?.serializeAccount() ?: return mainElement
    mainElement.addContent(userElement)
    return mainElement
  }

  override fun loadState(settings: Element) {
    LOG.info("Loading Hyperskill settings from storage")
    XmlSerializer.deserializeInto(this, settings)
    val accountClass = HyperskillAccount::class.java
    val user = settings.getChild(accountClass.simpleName)
    val loadedAccount = user.deserializeOAuthAccount(accountClass, HyperskillUserInfo::class.java)
    if (loadedAccount != null) {
      LOG.info("Loaded Hyperskill account from storage: userId=${loadedAccount.userInfo?.id ?: "unknown"}")
    }
    else {
      LOG.info("No Hyperskill account found in storage")
    }
    account = loadedAccount
  }

  companion object {
    val INSTANCE: HyperskillSettings
      get() = service()

    val LOGGED_IN_TO_HYPERSKILL: Topic<EduLogInListener> = createTopic("HYPERSKILL_LOGGED_IN")
  }
}
