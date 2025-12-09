package org.hyperskill.academy.ai.debugger.core.host

import com.intellij.openapi.util.NlsContexts
import org.hyperskill.academy.ai.debugger.core.AI_DEBUGGER_SERVICE_URL
import org.hyperskill.academy.ai.debugger.core.AI_DEBUGGER_STAGING_URL
import org.hyperskill.academy.ai.debugger.core.messages.AIDebuggerCoreBundle
import org.hyperskill.academy.ai.debugger.core.messages.BUNDLE
import org.hyperskill.academy.learning.actions.changeHost.ServiceHostEnum
import org.hyperskill.academy.learning.actions.changeHost.ServiceHostManager
import org.jetbrains.annotations.PropertyKey


enum class AIDebuggerServiceHost(
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String,
) : ServiceHostEnum {
  PRODUCTION("ai.debugger.service.production.server"),
  STAGING("ai.debugger.service.staging.server"),
  OTHER("ai.debugger.service.other");

  override val url: String
    get() = when (this) {
      PRODUCTION -> AI_DEBUGGER_SERVICE_URL
      STAGING -> AI_DEBUGGER_STAGING_URL
      OTHER -> "http://localhost:8080"
    }

  override fun visibleName(): @NlsContexts.ListItem String = AIDebuggerCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<AIDebuggerServiceHost>("AI Debugger Service", AIDebuggerServiceHost::class.java) {
    override val default: AIDebuggerServiceHost = PRODUCTION
    override val other: AIDebuggerServiceHost = OTHER
  }
}