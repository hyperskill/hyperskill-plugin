package org.hyperskill.academy.learning.stepik.hyperskill.metrics.handlers

import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import org.hyperskill.academy.learning.stepik.hyperskill.metrics.HyperskillMetricsService

object HyperskillFrontendEventsHandler : HyperskillEventsHandler<HyperskillFrontendEvent> {
  override val pendingEvents: List<HyperskillFrontendEvent>
    get() = HyperskillMetricsService.getInstance().allFrontendEvents(true)

  override fun sendEvents(events: List<HyperskillFrontendEvent>): Result<Any, String> {
    return HyperskillConnector.getInstance().sendFrontendEvents(events)
  }

  override fun addPendingEvents(events: List<HyperskillFrontendEvent>) {
    HyperskillMetricsService.getInstance().addAllFrontendEvents(events)
  }
}