package org.hyperskill.academy.ai.debugger.core.host

import com.intellij.openapi.util.NlsContexts
import org.hyperskill.academy.ai.debugger.core.AI_DEBUGGER_SERVICE_URL
import org.hyperskill.academy.ai.debugger.core.AI_DEBUGGER_STAGING_URL
import org.hyperskill.academy.ai.debugger.core.messages.AIDebuggerCoreBundle
import org.hyperskill.academy.ai.debugger.core.messages.BUNDLE
import org.hyperskill.academy.learning.actions.changeHost.ServiceHostEnum
import org.hyperskill.academy.learning.actions.changeHost.ServiceHostManager
import org.jetbrains.annotations.PropertyKey


@Suppress("unused") // All enum values ar used in UI
enum class AIDebuggerServiceHost(
  override val url: String,
  @param:PropertyKey(resourceBundle = BUNDLE) private val visibleNameKey: String
) : ServiceHostEnum {
  PRODUCTION(AI_DEBUGGER_SERVICE_URL, "ai.debugger.service.production.server"),
  STAGING(AI_DEBUGGER_STAGING_URL, "ai.debugger.service.staging.server"),
  OTHER("http://localhost:8080", "ai.debugger.service.other");

  override fun visibleName(): @NlsContexts.ListItem String = AIDebuggerCoreBundle.message(visibleNameKey)

  companion object : ServiceHostManager<AIDebuggerServiceHost>("AI Debugger Service", AIDebuggerServiceHost::class.java) {
    override val default: AIDebuggerServiceHost = PRODUCTION
    override val other: AIDebuggerServiceHost = OTHER
  }
}