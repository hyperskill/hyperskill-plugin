package org.hyperskill.academy.ai.debugger.core.breakpoint

import com.intellij.lang.Language
import com.intellij.lang.LanguageExtension
import com.intellij.xdebugger.breakpoints.XBreakpointProperties
import com.intellij.xdebugger.breakpoints.XLineBreakpointType

interface BreakpointTypeManager {
  fun getBreakPointType(): XLineBreakpointType<XBreakpointProperties<*>>

  companion object {
    private val EP_NAME = LanguageExtension<BreakpointTypeManager>("HyperskillEducational.breakpointTypeManager")

    fun getInstanceOrNull(language: Language): BreakpointTypeManager? =
      EP_NAME.forLanguage(language) ?: EP_NAME.forLanguage(Language.ANY)

    fun getInstance(language: Language): BreakpointTypeManager =
      requireNotNull(getInstanceOrNull(language)) {
        "No BreakpointTypeManager registered for language '${language.id}'. " +
        "Please register an implementation via 'HyperskillEducational.breakpointTypeManager'."
      }
  }
}
