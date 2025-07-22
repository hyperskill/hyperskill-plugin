package org.hyperskill.academy.learning.stepik.hyperskill.metrics.handlers

import org.hyperskill.academy.learning.Result

interface HyperskillEventsHandler<Event> {
  val pendingEvents: List<Event>

  fun sendEvents(events: List<Event>): Result<Any, String>

  fun addPendingEvents(events: List<Event>)
}