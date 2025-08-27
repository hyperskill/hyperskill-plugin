package org.hyperskill.academy.learning.stepik.hyperskill

import okhttp3.mockwebserver.MockResponse
import org.hyperskill.academy.learning.EduTestCase
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.messages.EduFormatBundle
import org.hyperskill.academy.learning.stepik.api.StepikBasedSubmission
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.MockHyperskillConnector
import org.junit.Test
import java.net.HttpURLConnection.*

class HyperskillHttpErrorsTest : EduTestCase() {
  private val mockConnector: MockHyperskillConnector get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  @Test
  fun `test service is under maintenance`() = doTest(HTTP_BAD_GATEWAY, EduFormatBundle.message("error.service.maintenance"))

  @Test
  fun `test service is down`() = doTest(HTTP_GATEWAY_TIMEOUT, EduFormatBundle.message("error.service.down"))

  @Test
  fun `test unexpected error occurred`() = doTest(HTTP_BAD_REQUEST, EduFormatBundle.message("error.unexpected.error", ""))

  @Test
  fun `test forbidden`() = doTest(HTTP_FORBIDDEN, EduFormatBundle.message("error.access.denied"))

  private fun doTest(code: Int, expectedError: String) {
    mockConnector.withResponseHandler(testRootDisposable) { _, _ -> MockResponse().setResponseCode(code) }
    val response = mockConnector.postSubmission(StepikBasedSubmission())
    val actualError = (response as Err).error
    assertTrue("Unexpected error message: `$actualError`", actualError.startsWith(expectedError))
  }
}
