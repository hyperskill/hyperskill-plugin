package org.hyperskill.academy.ai.debugger.core.action

import org.hyperskill.academy.ai.debugger.core.host.AIDebuggerServiceHost
import org.hyperskill.academy.learning.actions.changeHost.ChangeServiceHostAction
import org.jetbrains.annotations.NonNls

class AIDebuggerServiceChangeHost : ChangeServiceHostAction<AIDebuggerServiceHost>(AIDebuggerServiceHost) {
  companion object {
    @NonNls
    const val ACTION_ID = "HyperskillEducational.AIDebuggerServiceChangeHost"
  }
}