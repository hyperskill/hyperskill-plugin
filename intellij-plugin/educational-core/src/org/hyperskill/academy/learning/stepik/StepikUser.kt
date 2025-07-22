package org.hyperskill.academy.learning.stepik

import com.intellij.openapi.util.NlsSafe
import com.intellij.util.xmlb.annotations.Transient
import org.hyperskill.academy.learning.authUtils.OAuthAccount

class StepikUser : OAuthAccount<StepikUserInfo>() {
  @Suppress("UnstableApiUsage")
  override val servicePrefix: @NlsSafe String = StepikNames.STEPIK

  @get:Transient
  val id: Int
    get() {
      return userInfo.id
    }

  @get:Transient
  val name: String
    get() {
      return arrayOf(userInfo.firstName, userInfo.lastName).joinToString(" ")
    }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || javaClass != other.javaClass) return false
    val user = other as StepikUser
    val otherInfo = user.userInfo
    return userInfo == otherInfo
  }

  override fun hashCode(): Int = userInfo.hashCode()
}
