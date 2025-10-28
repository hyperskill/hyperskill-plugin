package org.hyperskill.academy.ai.debugger.core.feedback

import com.jetbrains.educational.ml.debugger.dto.Breakpoint
import com.jetbrains.educational.ml.debugger.response.BreakpointHintDetails
import org.hyperskill.academy.ai.debugger.core.service.TestInfo
import org.hyperskill.academy.learning.courseFormat.tasks.Task

data class AIDebugContext(
  val task: Task,
  val userSolution: Map<String, String>,
  val testInfo: TestInfo,
  val finalBreakpoints: List<Breakpoint>,
  val intermediateBreakpoints: Map<String, List<Int>>,
  val breakpointHints: List<BreakpointHintDetails>
)
