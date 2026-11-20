package org.hyperskill.academy.learning.stepik.hyperskill.api

import java.net.HttpURLConnection.HTTP_BAD_REQUEST
import java.net.HttpURLConnection.HTTP_ENTITY_TOO_LARGE

/**
 * Why a batch of metrics events was not delivered.
 * Pending events are sent again later, so the caller has to know whether trying again can help.
 */
sealed class HyperskillEventsSendError(val message: String) {

  /**
   * The server refused the events themselves, e.g. because they failed validation.
   * The same batch can never be accepted, so it must not be sent again.
   */
  class Rejected(val code: Int, responseBody: String) : HyperskillEventsSendError(responseBody)

  /**
   * The batch was not delivered for a reason unrelated to its content:
   * no connection, an expired session, rate limiting or a server failure. Sending it again later may succeed.
   */
  class Failed(message: String) : HyperskillEventsSendError(message)

  companion object {
    private const val HTTP_UNPROCESSABLE_ENTITY = 422

    /**
     * Codes that judge the request content. Any other code is about the server or the session,
     * e.g. 401 after a failed token refresh or 429, and is worth retrying.
     */
    val REJECTION_CODES: Set<Int> = setOf(HTTP_BAD_REQUEST, HTTP_ENTITY_TOO_LARGE, HTTP_UNPROCESSABLE_ENTITY)
  }
}
