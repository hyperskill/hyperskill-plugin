package org.hyperskill.academy.remote

import com.intellij.openapi.components.service
import com.jetbrains.rdserver.unattendedHost.UnattendedHostManager
import org.hyperskill.academy.learning.RemoteEnvHelper

class RemoteEnvDefaultHelper : RemoteEnvHelper {
  override fun isRemoteServer(): Boolean = UnattendedHostManager.getInstance().isUnattendedMode

  override fun getUserUidToken(): String? = service<EduRemoteUidHolderService>().userUid
}