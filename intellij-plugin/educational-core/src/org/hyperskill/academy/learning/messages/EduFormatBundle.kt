package org.hyperskill.academy.learning.messages

import org.hyperskill.academy.learning.courseFormat.FORMAT_BUNDLE
import org.jetbrains.annotations.PropertyKey

object EduFormatBundle : EduBundle(FORMAT_BUNDLE) {

  fun message(@PropertyKey(resourceBundle = FORMAT_BUNDLE) key: String, vararg params: Any): String {
    return getMessage(key, *params)
  }
}
