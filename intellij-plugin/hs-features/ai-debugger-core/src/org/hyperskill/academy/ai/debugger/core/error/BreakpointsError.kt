package org.hyperskill.academy.ai.debugger.core.error

import org.hyperskill.academy.ai.debugger.core.messages.BUNDLE
import org.jetbrains.annotations.PropertyKey

enum class BreakpointsError(@PropertyKey(resourceBundle = BUNDLE) override val messageKey: String) : AIDebuggerServiceError {
  // TODO: add more types of errors
  NO_BREAKPOINTS("action.HyperskillEducational.AiDebuggerNotification.no.suitable.breakpoints.found"),
  DEFAULT_ERROR("action.HyperskillEducational.AiDebuggerNotification.modal.session.fail");
}
