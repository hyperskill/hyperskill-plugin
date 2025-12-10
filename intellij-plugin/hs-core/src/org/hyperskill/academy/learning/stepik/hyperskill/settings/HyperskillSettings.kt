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
          LOG.info("Publishing LOGGED_IN_TO_HYPERSKILL event")
          ApplicationManager.getApplication().messageBus.syncPublisher(LOGGED_IN_TO_HYPERSKILL).userLoggedIn()
          LOG.info("LOGGED_IN_TO_HYPERSKILL event published")
        }
        else {
          notifyUserLoggedOut()
        }
      }
    }

  var updateAutomatically: Boolean = true

  override fun getState(): Element? {
    LOG.info("Saving Hyperskill settings to storage, account=${account?.userInfo?.id ?: "null"}")
    val mainElement = Element(serviceName)
    XmlSerializer.serializeInto(this, mainElement)
    val userElement = account?.serializeAccount()
    if (userElement == null) {
      LOG.info("Account serialization returned null, only basic settings will be saved")
      return mainElement
    }
    mainElement.addContent(userElement)
    LOG.info("Account serialized successfully")
    return mainElement
  }

  override fun loadState(settings: Element) {
    LOG.info("Loading Hyperskill settings from storage")
    XmlSerializer.deserializeInto(this, settings)
    val accountClass = HyperskillAccount::class.java
    val accountElement = settings.getChild(accountClass.simpleName)
    if (accountElement == null) {
      LOG.info("No HyperskillAccount element found in storage")
      account = null
      return
    }
    val loadedAccount = accountElement.deserializeOAuthAccount(accountClass, HyperskillUserInfo::class.java)
    if (loadedAccount != null) {
      LOG.info("Loaded Hyperskill account from storage: userId=${loadedAccount.userInfo?.id ?: "unknown"}")
    }
    else {
      LOG.info("Failed to deserialize Hyperskill account from storage")
    }
    account = loadedAccount
  }

  companion object {
    val INSTANCE: HyperskillSettings
      get() = service()

    val LOGGED_IN_TO_HYPERSKILL: Topic<EduLogInListener> = createTopic("HYPERSKILL_LOGGED_IN")
  }
}
