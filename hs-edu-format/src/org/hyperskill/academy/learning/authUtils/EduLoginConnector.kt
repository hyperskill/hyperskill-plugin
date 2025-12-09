package org.hyperskill.academy.learning.authUtils

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.ConnectionPool
import okhttp3.Interceptor
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.CODE_ARGUMENT
import org.hyperskill.academy.learning.courseFormat.EduFormatNames.REST_PREFIX
import org.hyperskill.academy.learning.courseFormat.UserInfo
import org.hyperskill.academy.learning.network.createRetrofitBuilder
import org.jetbrains.annotations.NonNls
import retrofit2.converter.jackson.JacksonConverterFactory
import java.util.regex.Pattern

/**
 * Base class for all connectors managing login.
 * Connectors, using [Authorization Code Flow](https://auth0.com/docs/get-started/authentication-and-authorization-flow/authorization-code-flow)
 * should be inherited from [org.hyperskill.academy.learning.api.EduOAuthCodeFlowConnector]
 */
abstract class EduLoginConnector<UserAccount : Account<*>, SpecificUserInfo : UserInfo> {
  open var account: UserAccount? = null

  protected abstract val baseUrl: String

  protected abstract val clientId: String

  abstract val objectMapper: ObjectMapper

  /**
   * Name of the platform used for developing purposes
   */
  protected abstract val platformName: String
    @NonNls get

  protected val connectionPool: ConnectionPool = ConnectionPool()

  protected val converterFactory: JacksonConverterFactory by lazy {
    JacksonConverterFactory.create(objectMapper)
  }

  abstract fun doAuthorize(
    vararg postLoginActions: Runnable,
    authorizationPlace: AuthorizationPlace = AuthorizationPlace.UNKNOWN
  )

  /**
   * Must be changed only with synchronization
   */
  protected var authorizationPlace: AuthorizationPlace? = null

  protected open val requestInterceptor: Interceptor? = null

  /**
   * Service name used for OAuth redirect URI.
   * Format: "hyperskill"
   */
  open val oAuthServiceName: String by lazy {
    platformName.lowercase()
  }

  open val oAuthServicePath: String by lazy {
    "/$REST_PREFIX/$oAuthServiceName/$OAUTH_SUFFIX"
  }

  abstract fun getCurrentUserInfo(): SpecificUserInfo?

  protected fun getEduOAuthEndpoints(): EduOAuthEndpoints =
    createRetrofitBuilder(baseUrl.withTrailingSlash(), connectionPool)
      .addConverterFactory(converterFactory)
      .build()
      .create(EduOAuthEndpoints::class.java)

  /**
   * No need to pass any arguments by default, but you need to pass
   * account and accessToken for [org.hyperskill.academy.learning.api.EduOAuthCodeFlowConnector.getUserInfo]
   * because access token is not saved at the moment we want to get userInfo and check if current user isGuest
   */
  protected inline fun <reified Endpoints> getEndpoints(
    account: UserAccount? = this.account,
    accessToken: String?,
    baseUrl: String = this.baseUrl
  ): Endpoints {

    val freshAccessToken: String? = getFreshAccessToken(account, accessToken)

    return createRetrofitBuilder(baseUrl.withTrailingSlash(), connectionPool, freshAccessToken, customInterceptor = requestInterceptor)
      .addConverterFactory(converterFactory)
      .build()
      .create(Endpoints::class.java)
  }

  abstract fun getFreshAccessToken(userAccount: UserAccount?, accessToken: String?): String?

  /**
   * Returns a pattern that matches OAuth service path.
   * Path: /api/hyperskill/oauth
   */
  fun getOAuthPattern(suffix: String = """\?$CODE_ARGUMENT=[\w+,.-]*\&$STATE=[\w+,.-]*"""): Pattern {
    return "^.*$oAuthServicePath$suffix".toPattern()
  }

  /**
   * Returns a pattern that matches the service name.
   * Path: /api/hyperskill
   */
  fun getServicePattern(suffix: String): Pattern = "^.*$oAuthServiceName$suffix".toPattern()

  open fun isLoggedIn(): Boolean = account != null

  companion object {
    @JvmStatic
    @NonNls
    protected val OAUTH_SUFFIX: String = "oauth"
    const val STATE: String = "state"

    /**
     * Retrofit builder needs url to be with trailing slash
     */
    protected fun String.withTrailingSlash(): String = if (!endsWith('/')) "$this/" else this
  }
}