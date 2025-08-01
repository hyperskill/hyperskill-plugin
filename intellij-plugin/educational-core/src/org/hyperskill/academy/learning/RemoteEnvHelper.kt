package org.hyperskill.academy.learning

import com.intellij.openapi.extensions.ExtensionPointName

/**
 * This extension must be implemented only in one place
 * @see [org.hyperskill.academy.remote.RemoteEnvDefaultHelper]
 */
interface RemoteEnvHelper {
  fun isRemoteServer(): Boolean

  /**
   * This token identifies the user logged into JBA and is used for authorization in the Submission service
   */
  fun getUserUidToken(): String?

  companion object {
    val EP_NAME: ExtensionPointName<RemoteEnvHelper> = ExtensionPointName.create("HyperskillEducational.remoteEnvHelper")

    fun isRemoteDevServer(): Boolean = EP_NAME.computeSafeIfAny { it.isRemoteServer() } == true

  }
}