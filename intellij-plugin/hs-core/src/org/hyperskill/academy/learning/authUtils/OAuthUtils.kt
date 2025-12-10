package org.hyperskill.academy.learning.authUtils

import com.google.common.collect.ImmutableMap
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.ui.Messages
import com.intellij.util.ReflectionUtil
import com.intellij.util.xmlb.SkipDefaultValuesSerializationFilters
import com.intellij.util.xmlb.XmlSerializer
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.EduUtilsKt
import org.hyperskill.academy.learning.courseFormat.UserInfo
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.serialization.CompositeSerializationFilter
import org.hyperskill.academy.learning.serialization.TransientFieldSerializationFilter
import org.jdom.Element
import org.jetbrains.builtInWebServer.BuiltInServerOptions
import org.jetbrains.ide.BuiltInServerManager
import java.io.IOException
import java.nio.charset.StandardCharsets

private val LOG: Logger = logger<OAuthUtils>()

object OAuthUtils {
  private val LOG: Logger = logger<OAuthUtils>()

  private const val SERVICE_DISPLAY_NAME_PREFIX = "EduTools"
  private const val OAUTH_OK_PAGE = "/oauthResponsePages/okPage.html"
  private const val OAUTH_ERROR_PAGE = "/oauthResponsePages/errorPage.html"
  private const val IDE_NAME = "%IDE_NAME"
  private const val PLATFORM_NAME = "%PLATFORM_NAME"
  private const val ERROR_MESSAGE = "%ERROR_MESSAGE"

  @Throws(IOException::class)
  fun getErrorPageContent(platformName: String, errorMessage: String): String {
    return getPageContent(
      OAUTH_ERROR_PAGE, ImmutableMap.of(
        ERROR_MESSAGE, errorMessage,
        PLATFORM_NAME, platformName
      )
    )
  }

  @Throws(IOException::class)
  private fun getPageContent(pagePath: String, replacements: Map<String, String>): String {
    var pageTemplate = getPageTemplate(pagePath)
    for ((key, value) in replacements) {
      pageTemplate = pageTemplate.replace(key.toRegex(), value)
    }
    return pageTemplate
  }

  @Throws(IOException::class)
  private fun getPageTemplate(pagePath: String): String {
    EduUtilsKt::class.java.getResourceAsStream(pagePath).use { pageTemplateStream ->
      return pageTemplateStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
    }
  }

  fun checkBuiltinPortValid(): Boolean {
    val port = BuiltInServerManager.getInstance().port
    val isPortValid = isBuiltinPortValid(port)
    if (!isPortValid) {
      showUnsupportedPortError(port)
    }
    return isPortValid
  }

  fun isBuiltinPortValid(port: Int): Boolean {
    val defaultPort = BuiltInServerOptions.DEFAULT_PORT

    // 20 port range comes from org.jetbrains.ide.BuiltInServerManagerImplKt.PORTS_COUNT
    val portsRange = defaultPort..defaultPort + 20
    val isValid = port in portsRange
    if (!isValid) {
      LOG.warn("Built-in port $port is not valid, because it's outside of default port range $portsRange")
    }
    return isValid
  }

  private fun showUnsupportedPortError(port: Int) {
    Messages.showErrorDialog(
      EduCoreBundle.message("error.unsupported.port.message", port.toString(), EduNames.OUTSIDE_OF_KNOWN_PORT_RANGE_URL),
      EduCoreBundle.message("error.authorization.error")
    )
  }

  fun credentialAttributes(userName: String, serviceName: String): CredentialAttributes =
    CredentialAttributes(generateServiceName("$SERVICE_DISPLAY_NAME_PREFIX $serviceName", userName))

  object GrantType {
    const val AUTHORIZATION_CODE = "authorization_code"
    const val REFRESH_TOKEN = "refresh_token"
  }
}

fun <UInfo : UserInfo> Account<UInfo>.serializeAccount(): Element? {
  val passwordSafe = PasswordSafe.instance
  LOG.info("Serializing account: isMemoryOnly=${passwordSafe.isMemoryOnly}, userInfo=${userInfo?.getFullName() ?: "null"}")
  if (passwordSafe.isMemoryOnly) {
    LOG.warn("PasswordSafe is in memory-only mode, account will not be persisted. " +
             "User should check Settings > Appearance & Behavior > System Settings > Passwords and select a persistent storage option.")
    return null
  }
  val currentUserInfo = userInfo
  if (currentUserInfo == null) {
    LOG.warn("Account has no userInfo, cannot serialize")
    return null
  }
  // Use TransientFieldSerializationFilter to skip fields marked with @field:Transient (like userInfo).
  // The standard IntelliJ XmlSerializer only recognizes com.intellij.util.xmlb.annotations.Transient,
  // but we use kotlin.jvm.Transient which compiles to Java's transient modifier.
  @Suppress("DEPRECATION")
  val serializationFilter = CompositeSerializationFilter(
    TransientFieldSerializationFilter,
    SkipDefaultValuesSerializationFilters()
  )
  val accountElement = XmlSerializer.serialize(this, serializationFilter)
  // Serialize userInfo fields directly into accountElement
  XmlSerializer.serializeInto(currentUserInfo, accountElement)
  return accountElement
}

fun <UserAccount : Account<UInfo>, UInfo : UserInfo> Element.deserializeAccount(
  accountClass: Class<UserAccount>,
  userInfoClass: Class<UInfo>
): UserAccount {
  // Remove userInfo element before deserializing Account to avoid XmlSerializer trying
  // to instantiate the UserInfo interface. The kotlin.jvm.Transient annotation on
  // Account.userInfo is not recognized by IntelliJ XmlSerializer.
  val userInfoElement = children.find { it.name == "option" && it.getAttributeValue("name") == "userInfo" }
  if (userInfoElement != null) {
    removeContent(userInfoElement)
  }

  val account = XmlSerializer.deserialize(this, accountClass)

  val userInfo = ReflectionUtil.newInstance(userInfoClass)
  XmlSerializer.deserializeInto(userInfo, this)
  account.userInfo = userInfo

  return account
}

fun <OAuthAcc : OAuthAccount<UInfo>, UInfo : UserInfo> Element.deserializeOAuthAccount(
  accountClass: Class<OAuthAcc>,
  userInfoClass: Class<UInfo>
): OAuthAcc? {

  val account = deserializeAccount(accountClass, userInfoClass)

  val tokenInfo = TokenInfo()
  XmlSerializer.deserializeInto(tokenInfo, this)

  // Security check: if accessToken was serialized to XML (insecure storage),
  // reject the account. Tokens should only be stored in PasswordSafe.
  if (tokenInfo.accessToken.isNotEmpty()) {
    LOG.warn("Access token found in XML storage (security violation), rejecting account deserialization")
    return null
  }
  LOG.info("Account deserialized successfully: ${accountClass.simpleName}")
  return account
}
