package org.hyperskill.academy.ai.debugger.jvm

import com.intellij.debugger.ui.breakpoints.Breakpoint
import com.intellij.debugger.ui.breakpoints.JavaLineBreakpointType
import com.intellij.openapi.project.Project
import com.intellij.xdebugger.breakpoints.XBreakpoint
import org.hyperskill.academy.ai.debugger.core.messages.AIDebuggerCoreBundle
import org.hyperskill.academy.ai.debugger.core.ui.AIDebuggerIcons
import org.jetbrains.java.debugger.breakpoints.properties.JavaLineBreakpointProperties
import javax.swing.Icon

class JvmAIDebuggerBreakpointType :
  JavaLineBreakpointType(BREAKPOINT_TYPE_ID, AIDebuggerCoreBundle.message("ai.debugger.jvm.breakpoint.description")) {
  override fun getEnabledIcon(): Icon = AIDebuggerIcons.Bug

  override fun createJavaBreakpoint(
    project: Project?,
    breakpoint: XBreakpoint<JavaLineBreakpointProperties>?
  ): Breakpoint<JavaLineBreakpointProperties> {
    return JvmAIDebuggerLineBreakpoint(project, breakpoint)
  }

  companion object {
    private const val BREAKPOINT_TYPE_ID: String = "jvm-line-ai"
  }
}
