package org.hyperskill.academy.ai.debugger.core.error

import org.hyperskill.academy.ai.debugger.core.messages.AIDebuggerCoreBundle
import org.jetbrains.annotations.NonNls

interface AIDebuggerServiceError {
  val messageKey: String

  fun message(): @NonNls String = AIDebuggerCoreBundle.message(messageKey)
}