package org.hyperskill.academy.learning.authUtils

import com.intellij.ide.util.PropertiesComponent
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.http.FullHttpRequest
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import org.hyperskill.academy.learning.authUtils.OAuthUtils.getErrorPageContent
import org.jetbrains.ide.RestService
import org.jetbrains.io.send
import java.io.IOException

// Should be implemented to handle oauth redirect to localhost:<port>
// and get the authorization code for different oauth providers
abstract class OAuthRestService(protected val platformName: String) : RestService() {
  @Throws(IOException::class)
  protected fun sendErrorResponse(
    request: HttpRequest,
    context: ChannelHandlerContext,
    errorMessage: String
  ): String {
    LOG.warn("$platformName: $errorMessage")
    showErrorPage(request, context, errorMessage)
    return errorMessage
  }

  @Throws(IOException::class)
  protected fun showErrorPage(request: HttpRequest, context: ChannelHandlerContext, errorMessage: String) {
    val pageContent = getErrorPageContent(platformName, errorMessage)
    createResponse(pageContent).send(context.channel(), request)
  }

  override fun isSupported(request: FullHttpRequest): Boolean = isRestServicesEnabled && super.isSupported(request)

  override fun isMethodSupported(method: HttpMethod): Boolean = method === HttpMethod.GET

  companion object {
    private const val IS_REST_SERVICES_ENABLED = "Edu.Stepik.RestServicesEnabled"
    var isRestServicesEnabled: Boolean
      get() = PropertiesComponent.getInstance().getBoolean(IS_REST_SERVICES_ENABLED, true)
      set(enabled) {
        PropertiesComponent.getInstance().setValue(IS_REST_SERVICES_ENABLED, enabled, true)
      }
  }
}
