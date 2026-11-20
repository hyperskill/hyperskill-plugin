package org.hyperskill.academy.learning.stepik.hyperskill.metrics

import com.intellij.concurrency.JobScheduler
import com.intellij.ide.AppLifecycleListener
import com.intellij.ide.plugins.DynamicPluginListener
import com.intellij.ide.plugins.IdeaPluginDescriptor
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.text.StringUtil
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.isUnitTestMode
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillEventsSendError
import org.hyperskill.academy.learning.stepik.hyperskill.metrics.handlers.HyperskillEventsHandler
import org.hyperskill.academy.learning.stepik.hyperskill.metrics.handlers.HyperskillFrontendEventsHandler
import org.hyperskill.academy.learning.stepik.hyperskill.metrics.handlers.HyperskillTimeSpentEventsHandler
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings
import org.jetbrains.annotations.VisibleForTesting
import java.util.concurrent.TimeUnit

class HyperskillMetricsScheduler : AppLifecycleListener, DynamicPluginListener {

  override fun appFrameCreated(commandLineArgs: MutableList<String>) = startSendingEvents()

  override fun pluginLoaded(pluginDescriptor: IdeaPluginDescriptor) {
    // Installing this plugin without an IDE restart creates no frame, so sending starts here.
    // The IDE reports every other plugin it loads as well, and none of them may start another sender
    if (pluginDescriptor.pluginId == PluginId.getId(EduNames.PLUGIN_ID)) {
      startSendingEvents()
    }
  }

  private fun startSendingEvents() {
    if (!HyperskillMetricsService.getInstance().markSendingStarted() || isUnitTestMode) {
      return
    }

    scheduleEventsSendingJob { sendFrontendEvents() }
    scheduleEventsSendingJob { sendTimeSpentEvents() }
  }

  companion object {
    private const val HYPERSKILL_STATISTICS_INTERVAL_REGISTRY: String = "hyperskill.metrics"

    @VisibleForTesting
    const val EVENTS_PER_REQUEST: Int = 1000

    private const val MAX_LOGGED_ERROR_LENGTH: Int = 500

    private val LOG: Logger = logger<HyperskillMetricsScheduler>()

    private fun <Event> sendEvents(eventsHandler: HyperskillEventsHandler<Event>) {
      val eventsHandlerName = eventsHandler.javaClass.simpleName

      if (HyperskillSettings.INSTANCE.account == null) {
        LOG.info("Hyperskill user logged out. No events were sent.")
        return
      }

      val pendingEvents = eventsHandler.pendingEvents
      if (pendingEvents.isEmpty()) {
        LOG.info("No data to send ($eventsHandlerName)")
        return
      }

      val remainingEvents = mutableListOf<Event>()

      for (eventsChunk in pendingEvents.chunked(EVENTS_PER_REQUEST)) {
        if (HyperskillSettings.INSTANCE.account == null) {
          remainingEvents.addAll(eventsChunk)
          continue
        }
        when (val res = eventsHandler.sendEvents(eventsChunk)) {
          is Ok -> {
            LOG.info("Successfully sent ${eventsChunk.size} events ($eventsHandlerName)")
            if (LOG.isDebugEnabled) { // check debug level so as not to serialize events if not needed
              LOG.debug("Events (${eventsHandlerName})=${eventsChunk.map { it }}")
            }
          }

          is Err -> when (val error = res.error) {
            // The same events would be rejected every time, so keeping them would resend them forever
            is HyperskillEventsSendError.Rejected -> LOG.warn(
              "Server rejected ${eventsChunk.size} events ($eventsHandlerName) with code ${error.code}, dropping them: ${error.message.forLog()}"
            )

            is HyperskillEventsSendError.Failed -> {
              LOG.info("Failed to send events ($eventsHandlerName), will retry later: ${error.message.forLog()}")
              remainingEvents.addAll(eventsChunk)
            }
          }
        }
      }

      if (remainingEvents.isNotEmpty()) {
        eventsHandler.addPendingEvents(remainingEvents)
      }
    }

    private fun scheduleEventsSendingJob(sendEvents: () -> Unit) {
      val job = JobScheduler.getScheduler().scheduleWithFixedDelay(
        { sendIgnoringExceptions(sendEvents) }, 0,
        Registry.intValue(HYPERSKILL_STATISTICS_INTERVAL_REGISTRY).toLong(),
        TimeUnit.MINUTES
      )
      Disposer.register(HyperskillMetricsService.getInstance()) { job.cancel(false) }
    }

    /**
     * An exception that escapes a periodic job cancels all its later runs,
     * and events would stay unsent until the IDE restarts
     */
    private fun sendIgnoringExceptions(sendEvents: () -> Unit) {
      try {
        sendEvents()
      }
      catch (e: Exception) {
        if (e !is ControlFlowException) {
          LOG.warn("Failed to send Hyperskill events", e)
        }
      }
    }

    /** A rejection lists an error for every event of the batch, which is about 80 KB for a full one */
    private fun String.forLog(): String = StringUtil.first(this, MAX_LOGGED_ERROR_LENGTH, true)

    @VisibleForTesting
    fun sendFrontendEvents() = sendEvents(HyperskillFrontendEventsHandler)

    @VisibleForTesting
    fun sendTimeSpentEvents() = sendEvents(HyperskillTimeSpentEventsHandler)
  }
}
