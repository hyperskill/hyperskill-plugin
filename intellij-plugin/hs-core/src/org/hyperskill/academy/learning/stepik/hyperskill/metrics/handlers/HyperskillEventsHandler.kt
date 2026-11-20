package org.hyperskill.academy.learning.stepik.hyperskill.metrics.handlers

import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillEventsSendError

interface HyperskillEventsHandler<Event> {
  val pendingEvents: List<Event>

  fun sendEvents(events: List<Event>): Result<Any, HyperskillEventsSendError>

  fun addPendingEvents(events: List<Event>)
}