package org.hyperskill.academy.remote

import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class EduRemoteUidHolderService {
  var userUid: String? = null
    internal set
}