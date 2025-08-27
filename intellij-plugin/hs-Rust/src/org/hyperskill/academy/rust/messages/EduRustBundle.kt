package org.hyperskill.academy.rust.messages

import org.hyperskill.academy.learning.messages.EduBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.EduRustBundle"

object EduRustBundle : EduBundle(BUNDLE) {

  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }
}