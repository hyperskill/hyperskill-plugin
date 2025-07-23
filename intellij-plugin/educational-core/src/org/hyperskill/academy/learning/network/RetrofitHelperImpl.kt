package org.hyperskill.academy.learning.network

import com.intellij.openapi.application.ex.ApplicationUtil
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.util.PlatformUtils
import com.intellij.util.net.ssl.CertificateManager
import okhttp3.OkHttpClient
import org.hyperskill.academy.learning.*
import org.hyperskill.academy.learning.messages.EduFormatBundle
import org.hyperskill.academy.learning.newproject.CoursesDownloadingException
import org.hyperskill.academy.learning.stepik.StepikNames
import retrofit2.Call
import retrofit2.Response
import java.io.IOException
import java.io.InterruptedIOException
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ProxySelector
import java.net.URI

/**
 * This is a service class, NOT intended to be instantiated directly
 *
 * @see org.hyperskill.academy.learning.network.RetrofitHelper
 * @see org.hyperskill.academy.learning.findService
 */
class RetrofitHelperImpl : RetrofitHelper {
  override fun <T> executeCall(call: Call<T>, omitErrors: Boolean): Result<Response<T>, String> {
    return try {
      val progressIndicator = ProgressManager.getInstance().progressIndicator

      val response = if (progressIndicator != null) {
        ApplicationUtil.runWithCheckCanceled({ call.execute() }, progressIndicator)
      }
      else {
        call.execute()
      }

      ProgressManager.checkCanceled()
      Ok(response)
    }
    catch (e: InterruptedIOException) {
      log("Connection to server was interrupted", e.message, omitErrors)
      Err("${EduFormatBundle.message("error.connection.interrupted")}\n\n${e.message}")
    }
    catch (e: CoursesDownloadingException) {
      log("Failed to connect to server", e.message, true)
      throw e
    }
    catch (e: IOException) {
      log("Failed to connect to server", e.message, omitErrors)
      Err("${EduFormatBundle.message("error.failed.to.connect")} \n\n${e.message}")
    }
    catch (e: ProcessCanceledException) {
      // We don't have to LOG.log ProcessCanceledException:
      // 'Control-flow exceptions (like ProcessCanceledException) should never be LOG.logged: ignore for explicitly started processes or...'
      call.cancel()
      throw e
    }
    catch (e: RuntimeException) {
      log("Failed to connect to server", e.message, omitErrors)
      Err("${EduFormatBundle.message("error.failed.to.connect")}\n\n${e.message}")
    }
  }

  override fun customizeClient(builder: OkHttpClient.Builder, baseUrl: String): OkHttpClient.Builder {
    return builder
      .addProxy(baseUrl)
      .addEdtAssertions()
  }

  private fun OkHttpClient.Builder.addProxy(baseUrl: String): OkHttpClient.Builder {
    val uri = URI.create(baseUrl)
    val proxies = ProxySelector.getDefault().select(uri)
    val address = proxies.firstOrNull()?.address() as? InetSocketAddress
    if (address != null) {
      proxy(Proxy(Proxy.Type.HTTP, address))
    }
    val trustManager = CertificateManager.getInstance().trustManager
    val sslContext = CertificateManager.getInstance().sslContext
    return sslSocketFactory(sslContext.socketFactory, trustManager)
  }

  private fun OkHttpClient.Builder.addEdtAssertions(): OkHttpClient.Builder {
    return addInterceptor { chain ->
      NetworkRequestAssertionPolicy.assertIsDispatchThread()
      val request = chain.request()
      chain.proceed(request)
    }
  }

  @Suppress("UnstableApiUsage")
  override val eduToolsUserAgent: String
    get() {
      val version = pluginVersion(EduNames.PLUGIN_ID) ?: "unknown"

      return String.format(
        "%s/version(%s)/%s/%s", StepikNames.PLUGIN_NAME, version, System.getProperty("os.name"), PlatformUtils.getPlatformPrefix()
      )
    }

  private fun log(title: String, message: String?, optional: Boolean) {
    val fullText = "$title. $message"
    if (optional) LOG.warn(fullText) else LOG.error(fullText)
  }

  companion object {
    private val LOG = logger<RetrofitHelperImpl>()
  }
}
