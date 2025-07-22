package org.hyperskill.academy.php.messages

import org.hyperskill.academy.learning.messages.EduBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

@NonNls
private const val BUNDLE = "messages.EduPhpBundle"

object EduPhpBundle : EduBundle(BUNDLE) {
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }
}