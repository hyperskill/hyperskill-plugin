package org.hyperskill.academy.learning.stepik.hyperskill.projectOpen

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.util.concurrency.AppExecutorUtil
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Ok
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.stepik.hyperskill.courseGeneration.HyperskillOpenInIdeRequestHandler
import org.junit.Test

/**
 * Covers [HyperskillOpenInIdeRequestHandler.loadStagesWithTimeout]. The base class injects a
 * same-thread executor in setUp(), so `supplyAsync` runs the loader inline and the future resolves
 * immediately. This exercises the real future/timeout/cancellation code path that used to be dead
 * behind the removed `isUnitTestMode` short-circuit, without spawning background threads (except the
 * timeout case, which deliberately uses the application pool).
 */
class HyperskillLoadStagesWithTimeoutTest : HyperskillProjectOpenerTestBase() {

  @Test
  fun `test successful loading returns Ok`() {
    val result = HyperskillOpenInIdeRequestHandler.loadStagesWithTimeout(project, HyperskillCourse()) {
      // loader succeeds without doing any work
    }
    assertTrue("Expected Ok but was $result", result is Ok)
  }

  @Test
  fun `test loader failure returns Err`() {
    val result = HyperskillOpenInIdeRequestHandler.loadStagesWithTimeout(project, HyperskillCourse()) {
      throw IllegalStateException("boom")
    }
    assertTrue("Expected Err but was $result", result is Err)
  }

  @Test
  fun `test ProcessCanceledException from loader is rethrown`() {
    try {
      HyperskillOpenInIdeRequestHandler.loadStagesWithTimeout(project, HyperskillCourse()) {
        throw ProcessCanceledException()
      }
      fail("Expected ProcessCanceledException to be rethrown")
    }
    catch (expected: ProcessCanceledException) {
      // expected: a control-flow exception must propagate, not be swallowed into an Err
    }
  }

  @Test
  fun `test slow loading times out and returns Err`() {
    // Use a real background executor so the future is still running when get() times out.
    HyperskillOpenInIdeRequestHandler.stagesLoaderExecutor = AppExecutorUtil.getAppExecutorService()
    val result = HyperskillOpenInIdeRequestHandler.loadStagesWithTimeout(project, HyperskillCourse(), timeoutMs = 100L) {
      Thread.sleep(3_000)
    }
    assertTrue("Expected Err (timeout) but was $result", result is Err)
  }
}
