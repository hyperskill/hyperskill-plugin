package org.hyperskill.academy.ai.debugger.core.error

import org.hyperskill.academy.ai.debugger.core.messages.BUNDLE
import org.jetbrains.annotations.PropertyKey

enum class BreakpointHintError(@param:PropertyKey(resourceBundle = BUNDLE) override val messageKey: String) : AIDebuggerServiceError {
  // TODO: add more types of errors
  NO_BREAKPOINT_HINTS("action.HyperskillEducational.AiDebuggerNotification.no.suitable.breakpoint.hints.found"),
  DEFAULT_ERROR("action.HyperskillEducational.AiDebuggerNotification.modal.session.fail");
}