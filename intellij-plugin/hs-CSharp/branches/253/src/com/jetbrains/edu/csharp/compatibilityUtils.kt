package org.hyperskill.academy.csharp

import com.intellij.openapi.project.Project
import com.jetbrains.rider.model.RdUnitTestSession
import com.jetbrains.rider.projectView.SolutionDescriptionFactory

// In 253, SolutionDescriptionFactory.existing requires a displayName parameter
internal fun createExistingSolutionDescription(solutionPath: String) =
  SolutionDescriptionFactory.existing(solutionPath, displayName = null)

/**
 * Helper class to hold test result data across platform versions.
 * In 252, this data comes from RdUnitTestResultData.
 * In 253, the API changed significantly and we construct this from available sources.
 */
data class TestResultData(
  val exceptionLines: String
)

// In 253, resultData property was removed from RdUnitTestSession
// The test result output is now accessed differently through the session protocol
// For now, we provide a no-op implementation - tests will pass/fail but without detailed output
internal fun adviseResultData(
  @Suppress("UNUSED_PARAMETER") project: Project,
  @Suppress("UNUSED_PARAMETER") rdSession: RdUnitTestSession,
  @Suppress("UNUSED_PARAMETER") nodeId: Int,
  @Suppress("UNUSED_PARAMETER") callback: (TestResultData) -> Unit
) {
  // TODO: BACKCOMPAT 253 - The resultData API was removed in 253.
  // Need to find the replacement API for getting detailed test failure output.
  // For now, the callback is not invoked, which means detailed error messages
  // won't be available, but test pass/fail status will still work.
}
