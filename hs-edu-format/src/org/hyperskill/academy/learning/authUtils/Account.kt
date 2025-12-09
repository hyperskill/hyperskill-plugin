package org.hyperskill.academy.learning.authUtils

import org.hyperskill.academy.learning.courseFormat.UserInfo
import org.hyperskill.academy.learning.findService


abstract class Account<UInfo : UserInfo> {
  protected abstract val servicePrefix: String
  protected val serviceName get() = "$servicePrefix Integration"

  @Transient
  var userInfo: UInfo? = null

  abstract fun isUpToDate(): Boolean
  protected fun getUserName(): String = userInfo?.getFullName() ?: ""

  protected fun getSecret(userName: String, serviceNameForPasswordSafe: String): String? {
    return findService(PasswordService::class.java).getSecret(userName, serviceNameForPasswordSafe)
  }
}
