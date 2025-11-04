package org.hyperskill.academy.ai.debugger.core.messages

import  org.hyperskill.academy.learning.messages.EduBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

@NonNls
const val BUNDLE: String = "messages.AIDebuggerCoreBundle"

object AIDebuggerCoreBundle : EduBundle(BUNDLE) {
  fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): String = getMessage(key, *params)

  fun lazyMessage(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any): Supplier<String> =
    Supplier { getMessage(key, *params) }
}