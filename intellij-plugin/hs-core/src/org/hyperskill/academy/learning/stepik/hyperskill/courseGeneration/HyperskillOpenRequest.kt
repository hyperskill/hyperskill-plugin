package org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration

import org.hyperskill.academy.learning.courseGeneration.OpenInIdeRequest

/**
 * General interface for all Hyperskill Open In IDE requests
 */
sealed interface HyperskillOpenRequest : OpenInIdeRequest

/**
 * Open Step In IDE request. Problems are always opened in a separate problems project
 * (e.g. "Kotlin Problems"), regardless of the project selected on Hyperskill.
 */
class HyperskillOpenStepRequest(
  val stepId: Int,
  val language: String,
  val isLanguageSelectedByUser: Boolean = false
) : HyperskillOpenRequest {
  override fun toString(): String = "stepId=$stepId language=$language"
}

/**
 * Open Stage In IDE request when the user has selected Hyperskill project
 */
class HyperskillOpenProjectStageRequest(val projectId: Int, val stageId: Int?) : HyperskillOpenRequest {
  override fun toString(): String = "projectId=$projectId stageId=$stageId"
}
