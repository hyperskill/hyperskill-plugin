package org.hyperskill.academy.learning.checker

import org.hyperskill.academy.learning.checker.CheckUtils.CONGRATULATIONS
import org.hyperskill.academy.learning.checker.CheckUtils.fillWithIncorrect
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckResultDiff
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.xmlEscaped
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.VisibleForTesting
import java.util.regex.Pattern

class TestsOutputParser {

  private var pendingFailedTestMessage: StringBuilder = StringBuilder()
  private var lastFailedMessage: TestMessage.Failed? = null
  private var congratulations: String = CONGRATULATIONS

  fun getCheckResult(messages: List<String>, needEscapeResult: Boolean): CheckResult {
    val processor: (TestMessage) -> Unit = { message ->
      when (message) {
        is TestMessage.Congrats -> {
          congratulations = message.congratulations
        }

        is TestMessage.Failed -> {
          lastFailedMessage = message
        }

        else -> Unit
      }
    }

    for (message in messages) {
      processMessage(message, processor)
      if (lastFailedMessage != null) break
    }
    processPendingFailedMessage(processor)

    val finalFailedMessage = lastFailedMessage
    return if (finalFailedMessage != null) {
      val message = if (needEscapeResult) finalFailedMessage.message.xmlEscaped else finalFailedMessage.message
      CheckResult(CheckStatus.Failed, fillWithIncorrect(message), diff = finalFailedMessage.diff)
    }
    else {
      CheckResult(CheckStatus.Solved, congratulations)
    }
  }

  fun processMessage(message: @Nls String, processor: (TestMessage) -> Unit) {
    // Pass each line of output to processor as is to show them in console, for example
    processor(TestMessage.TextLine(message))
    if (!message.startsWith(STUDY_PREFIX)) {
      // If line doesn't started with STUDY_PREFIX then previous failed message is fully read
      // and can be processed
      processPendingFailedMessage(processor)
    }
    else {
      when {
        TEST_OK in message -> {
          // Process failed message accumulated in `pendingFailedTestMessage` buffer
          // since `message` is a new ok message
          processPendingFailedMessage(processor)
          val name = message.substringAfter("$STUDY_PREFIX ").substringBefore(" $TEST_OK")
          processor(TestMessage.Ok(name))
        }

        CONGRATS_MESSAGE in message -> {
          // Process failed message accumulated in `pendingFailedTestMessage` buffer
          // since `message` is a new congrats message
          processPendingFailedMessage(processor)
          processor(TestMessage.Congrats(message.substringAfter(CONGRATS_MESSAGE)))
        }

        TEST_FAILED in message -> {
          // Process failed message accumulated in `pendingFailedTestMessage` buffer
          // since `message` is the first line of new failed message
          processPendingFailedMessage(processor)
          pendingFailedTestMessage.append(message.substringAfter("$STUDY_PREFIX ").removeSuffix("\n"))
        }

        else -> {
          // Append secondary lines of multiline failed message
          pendingFailedTestMessage.append("\n")
          pendingFailedTestMessage.append(message.substringAfter("$STUDY_PREFIX ").removeSuffix("\n"))
        }
      }
    }
  }

  private fun processPendingFailedMessage(processor: (TestMessage) -> Unit) {
    if (pendingFailedTestMessage.isEmpty()) return
    val fullMessage = pendingFailedTestMessage.toString()
    // Our custom python test framework produces test name before `TEST_FAILED`
    val rawTestName = fullMessage.substringBefore(TEST_FAILED, "").trim()
    val testName = rawTestName.ifEmpty { "test" }
    val message = fullMessage.substringAfter(TEST_FAILED)
    val matcher = TEST_FAILED_PATTERN.matcher(message)
    val testMessage = if (matcher.find()) {
      val errorMessage = matcher.group(2) ?: ""
      val expectedText = matcher.group(3)
      val actual = matcher.group(4)
      TestMessage.Failed(testName, errorMessage, expectedText, actual)
    }
    else {
      TestMessage.Failed(testName, message, null, null)
    }
    pendingFailedTestMessage = StringBuilder()
    processor(testMessage)
  }

  private val TestMessage.Failed.diff: CheckResultDiff?
    get() =
      if (expected != null && actual != null) CheckResultDiff(expected, actual, message) else null

  companion object {
    const val STUDY_PREFIX = "#educational_plugin"

    private val TEST_FAILED_PATTERN: Pattern = Pattern.compile(
      "((.+) )?expected: ?(.*) but was: ?(.*)",
      Pattern.MULTILINE or Pattern.DOTALL
    )

    @VisibleForTesting
    const val TEST_OK = "test OK"

    @VisibleForTesting
    const val TEST_FAILED = "FAILED + "

    @VisibleForTesting
    const val CONGRATS_MESSAGE = "CONGRATS_MESSAGE "
  }

  sealed class TestMessage {
    class TextLine(val text: @Nls String) : TestMessage()
    class Ok(val testName: String) : TestMessage()
    class Failed(val testName: String, val message: @Nls String, val expected: String? = null, val actual: String? = null) : TestMessage()
    class Congrats(val congratulations: @Nls String) : TestMessage()
  }
}
