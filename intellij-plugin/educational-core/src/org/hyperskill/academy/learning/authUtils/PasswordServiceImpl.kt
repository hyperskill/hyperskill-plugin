package org.hyperskill.academy.learning.authUtils

import com.intellij.credentialStore.ACCESS_TO_KEY_CHAIN_DENIED
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import org.hyperskill.academy.learning.authUtils.OAuthUtils.credentialAttributes
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.notification.EduNotificationManager

class PasswordServiceImpl : PasswordService {
  override fun getSecret(userName: String, serviceNameForPasswordSafe: String): String? {
    val credentials = PasswordSafe.instance.get(credentialAttributes(userName, serviceNameForPasswordSafe)) ?: return null
    if (credentials == ACCESS_TO_KEY_CHAIN_DENIED) {
      EduNotificationManager.showErrorNotification(
        title = EduCoreBundle.message("notification.tokens.access.denied.title", userName),
        content = EduCoreBundle.message("notification.tokens.access.denied.text")
      )
      return null
    }
    return credentials.getPasswordAsString()
  }

  override fun saveSecret(userName: String, serviceNameForPasswordSafe: String, secret: String) {
    PasswordSafe.instance.set(credentialAttributes(userName, serviceNameForPasswordSafe), Credentials(userName, secret))
  }
}
