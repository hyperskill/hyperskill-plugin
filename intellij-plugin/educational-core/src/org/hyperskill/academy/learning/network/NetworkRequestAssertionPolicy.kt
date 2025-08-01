package org.hyperskill.academy.learning.network

import com.intellij.diagnostic.ThreadDumper
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Attachment
import com.intellij.openapi.diagnostic.RuntimeExceptionWithAttachments
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.registry.Registry
import org.hyperskill.academy.learning.isUnitTestMode

enum class NetworkRequestAssertionPolicy {
  OFF {
    override fun assert() {}
  },
  LOG_ERROR {
    override fun assert() {
      LOG.error(ASSERT_MESSAGE, threadDumpAttachment())
    }
  },
  EXCEPTION {
    override fun assert() {
      throw RuntimeExceptionWithAttachments(ASSERT_MESSAGE, threadDumpAttachment())
    }
  };

  protected abstract fun assert()

  protected fun threadDumpAttachment(): Attachment = Attachment("threadDump.txt", ThreadDumper.dumpThreadsToString())

  companion object {

    private const val ASSERT_MESSAGE = "Network requests from Event Dispatch Thread (EDT) are not allowed"

    private val LOG = logger<NetworkRequestAssertionPolicy>()

    fun assertIsDispatchThread() {
      // Someday we will remove `!isUnitTestMode` condition and start catching such things in tests as well
      if (ApplicationManager.getApplication().isDispatchThread && !isUnitTestMode) {
        val selectedOption = Registry.get("hyperskill.network.request.assertion.policy").selectedOption
        val currentAssertionPolicy = values().find { it.name == selectedOption } ?: LOG_ERROR
        currentAssertionPolicy.assert()
      }
    }
  }
}