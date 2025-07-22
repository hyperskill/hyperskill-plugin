package org.hyperskill.academy.learning.stepik

import com.fasterxml.jackson.annotation.JsonProperty
import org.hyperskill.academy.learning.courseFormat.EduFormatNames
import org.hyperskill.academy.learning.courseFormat.UserInfo
import org.hyperskill.academy.learning.stepik.api.FIRST_NAME
import org.hyperskill.academy.learning.stepik.api.IS_GUEST
import org.hyperskill.academy.learning.stepik.api.LAST_NAME
import org.jetbrains.annotations.TestOnly

class StepikUserInfo private constructor() : UserInfo {
  @JsonProperty(EduFormatNames.ID)
  var id = -1

  @JsonProperty(FIRST_NAME)
  var firstName = ""

  @JsonProperty(LAST_NAME)
  var lastName = ""

  @JsonProperty(IS_GUEST)
  var isGuest = false

  @TestOnly
  constructor(fullName: String) : this() {
    val firstLast = fullName.split(" ").toMutableList()
    if (firstLast.isEmpty()) {
      return
    }
    firstName = firstLast.removeFirst()
    if (firstLast.isNotEmpty()) {
      lastName = firstLast.joinToString(" ")
    }
  }

  override fun getFullName(): String = if (lastName.isNotEmpty()) "$firstName $lastName" else firstName

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as StepikUserInfo

    if (id != other.id) return false
    if (firstName != other.firstName) return false
    if (lastName != other.lastName) return false
    return isGuest == other.isGuest
  }

  override fun hashCode(): Int {
    var result = 31 * id + firstName.hashCode()
    result = 31 * result + lastName.hashCode()
    return result
  }

  override fun toString(): String = firstName
}
