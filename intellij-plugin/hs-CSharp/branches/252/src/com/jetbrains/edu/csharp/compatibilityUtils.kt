package org.hyperskill.academy.csharp

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.jetbrains.rider.model.RdUnitTestSession
import com.jetbrains.rider.projectView.SolutionDescriptionFactory

// In 252, SolutionDescriptionFactory.existing doesn't require displayName parameter
internal fun createExistingSolutionDescription(solutionPath: String) =
  SolutionDescriptionFactory.existing(solutionPath)

/**
 * Helper class to hold test result data across platform versions.
 * In 252, this data comes from RdUnitTestResultData.
 * In 253, the API changed significantly and we construct this from available sources.
 */
data class TestResultData(
  val exceptionLines: String
)

// In 252, resultData is IOptProperty<RdUnitTestResultData?>
internal fun adviseResultData(
  project: Project,
  rdSession: RdUnitTestSession,
  nodeId: Int,
  callback: (TestResultData) -> Unit
) {
  rdSession.resultData.advise(project.lifetime) { resultData ->
    if (resultData != null && resultData.nodeId == nodeId) {
      callback(TestResultData(exceptionLines = resultData.exceptionLines))
    }
  }
}
