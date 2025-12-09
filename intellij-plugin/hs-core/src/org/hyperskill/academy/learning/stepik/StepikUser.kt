package org.hyperskill.academy.learning.stepik

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Transient
import org.hyperskill.academy.learning.authUtils.OAuthAccount

class StepikUser : OAuthAccount<StepikUserInfo>() {
  @Suppress("UnstableApiUsage")
  override val servicePrefix: @NlsSafe String = StepikNames.STEPIK

  @get:Transient
  val id: Int
    get() = userInfo?.id ?: 0

  @get:Transient
  val name: String
    get() {
      val info = userInfo ?: return ""
      return arrayOf(info.firstName, info.lastName).joinToString(" ")
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val user = other as StepikUser
    return userInfo == user.userInfo
  }

  override fun hashCode(): Int = userInfo?.hashCode() ?: 0
}
