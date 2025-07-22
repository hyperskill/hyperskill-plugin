package org.hyperskill.academy.learning.stepik.hyperskill.metrics.handlers

import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillTimeSpentEvent
import org.hyperskill.academy.learning.stepik.hyperskill.metrics.HyperskillMetricsService

object HyperskillTimeSpentEventsHandler : HyperskillEventsHandler<HyperskillTimeSpentEvent> {
  override val pendingEvents: List<HyperskillTimeSpentEvent>
    get() = HyperskillMetricsService.getInstance().allTimeSpentEvents(reset = true)

  override fun sendEvents(events: List<HyperskillTimeSpentEvent>): Result<Any, String> =
    HyperskillConnector.getInstance().sendTimeSpentEvents(events)

  override fun addPendingEvents(events: List<HyperskillTimeSpentEvent>) {
    HyperskillMetricsService.getInstance().addAllTimeSpentEvents(events.associateBy({ it.step }, { it.duration }))
  }
}