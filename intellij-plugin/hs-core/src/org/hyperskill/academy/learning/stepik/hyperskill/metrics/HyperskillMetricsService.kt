package org.hyperskill.academy.learning.stepik.hyperskill.metrics

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.xmlb.annotations.XCollection
import org.hyperskill.academy.learning.EduTestAware
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.projectView.CourseViewUtils.isSolved
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillFrontendEvent
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillFrontendEventType
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillTimeSpentEvent
import org.jetbrains.annotations.TestOnly
import org.jetbrains.annotations.VisibleForTesting
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.DoubleAdder
import kotlin.math.min

@State(name = "HsMetrics", storages = [Storage("hyperskill.xml", roamingType = RoamingType.DISABLED)])
open class HyperskillMetricsService : PersistentStateComponent<HyperskillMetricsService.State>, Disposable, EduTestAware {
  private val frontendEvents: Deque<HyperskillFrontendEvent> = ConcurrentLinkedDeque()
  private val timeSpentEvents: MutableMap<Int, DoubleAdder> = mutableMapOf()
  private var taskInProgress: Pair<Int, Long>? = null
  private val sendingStarted = AtomicBoolean(false)

  private val lock = Object()

  fun viewEvent(task: Task?) {
    task?.course as? HyperskillCourse ?: return

    doAddViewEvent(task)
    taskStarted(task)
  }

  fun taskStarted(task: Task) {
    if (task.isSolved) {
      return
    }
    val id = task.id
    taskStarted(id)
  }

  @VisibleForTesting
  fun taskStarted(id: Int) {
    synchronized(lock) {

      // Stop tracking of previous task
      taskStopped()

      // There is no need to course task with corrupted 0 id
      if (id != 0) {
        taskInProgress = id to System.currentTimeMillis()
      }
    }
  }

  fun taskStopped() {
    synchronized(lock) {
      val (id, start) = taskInProgress ?: return
      val duration = toDuration(start)
      timeSpentEvents.computeIfAbsent(id) { DoubleAdder() }.add(duration)

      taskInProgress = null
    }
  }

  protected open fun toDuration(start: Long): Double = (System.currentTimeMillis() - start).toDouble() / 1000

  @VisibleForTesting
  fun doAddViewEvent(task: Task) {
    if (task.id == 0) return

    val event = HyperskillFrontendEvent().apply {
      route = task.getRoute()
      action = HyperskillFrontendEventType.VIEW
    }

    frontendEvents.add(event)
  }

  fun addAllFrontendEvents(pendingEvents: List<HyperskillFrontendEvent>) {
    pendingEvents.subList(0, min(FRONTEND_EVENTS_LIMIT, pendingEvents.size)).asReversed().forEach { frontendEvents.addFirst(it) }
  }

  /**
   * Returns the queued events, and with [emptyQueue] takes them out of the queue.
   *
   * A send may overlap with another send or with saving the state, and an overlapping reader takes everything it finds
   * in the queue as an event. So nothing but real events may be put there, not even a marker for the end of a snapshot
   */
  fun allFrontendEvents(emptyQueue: Boolean = true): List<HyperskillFrontendEvent> {
    if (!emptyQueue) {
      return frontendEvents.toList()
    }

    // Events added meanwhile wait for the next send.
    // pollFirst is atomic, so two overlapping sends never take the same event
    val count = frontendEvents.size
    val events = ArrayList<HyperskillFrontendEvent>(count)
    while (events.size < count) {
      events += frontendEvents.pollFirst() ?: break
    }
    return events
  }

  fun allTimeSpentEvents(reset: Boolean): List<HyperskillTimeSpentEvent> {
    return pendingTimeSpentEvents(reset).map {
      HyperskillTimeSpentEvent().apply {
        step = it.key
        duration = it.value
      }
    }
  }

  fun addAllTimeSpentEvents(pendingTimeSpentEvents: Map<Int, Double>) {
    for (event in pendingTimeSpentEvents) {
      val id = event.key
      val duration = event.value
      timeSpentEvents.computeIfAbsent(id) { DoubleAdder() }.add(duration)
    }
  }

  private fun pendingTimeSpentEvents(reset: Boolean = true): Map<Int, Double> {
    return timeSpentEvents.mapValues {
      val value = it.value
      if (reset) value.sumThenReset() else value.sum()
    }.filterValues { it != 0.0 }
  }

  override fun getState(): State {
    val pendingTimeSpentEvents = pendingTimeSpentEvents(reset = false)
    val pendingFrontendEvents = allFrontendEvents(false)

    return State().apply {
      events = pendingFrontendEvents.subList(0, min(pendingFrontendEvents.size, FRONTEND_EVENTS_LIMIT)).toMutableList()
      timeSpentEvents = pendingTimeSpentEvents.toMutableMap()
    }
  }

  override fun loadState(state: State) {
    // Older versions could save events without action and route. The server rejects every batch that contains one
    val (validEvents, invalidEvents) = state.events.partition { it.hasActionAndRoute() }
    if (invalidEvents.isNotEmpty()) {
      LOG.info("Dropped ${invalidEvents.size} saved events without action or route")
    }
    frontendEvents.addAll(validEvents)
    addAllTimeSpentEvents(state.timeSpentEvents)
  }

  /**
   * Returns `true` for the first call only.
   * Events must be sent by a single pair of jobs: overlapping sends multiply the requests to the server
   */
  fun markSendingStarted(): Boolean = sendingStarted.compareAndSet(false, true)

  @get:VisibleForTesting
  val isSendingStarted: Boolean get() = sendingStarted.get()

  override fun dispose() {
    // do nothing
  }

  @TestOnly
  override fun cleanUpState() {
    frontendEvents.clear()
    timeSpentEvents.clear()
    taskInProgress = null
    sendingStarted.set(false)
  }

  class State : BaseState() {
    @get:XCollection(style = XCollection.Style.v2)
    var events by list<HyperskillFrontendEvent>()

    @get:XCollection(style = XCollection.Style.v2)
    var timeSpentEvents by map<Int, Double>()
  }

  companion object {
    private val LOG: Logger = logger<HyperskillMetricsService>()

    fun getInstance(): HyperskillMetricsService = service()

    // it is approximately 300 bytes per event, lets keep hyperskill.xml file less than 2 MB (except time spent events)
    // time spent events are limited by the number of steps
    @VisibleForTesting
    const val FRONTEND_EVENTS_LIMIT: Int = 10000

    @VisibleForTesting
    fun Task.getRoute(): String {
      val course = course as? HyperskillCourse ?: error("Course is not a Hyperskill course")
      return if (course.isTaskInProject(this)) {
        val projectId = course.hyperskillProject?.id ?: error("Course doesn't have Hyperskill project")
        val stageId = course.stages[index - 1].id
        "/projects/$projectId/stages/$stageId/implement"
      }
      else {
        "/learn/step/$id"
      }
    }
  }
}