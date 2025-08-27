package org.hyperskill.academy.learning.stepik.hyperskill.api

import org.hyperskill.academy.learning.stepik.hyperskill.HYPERSKILL_URL

class HyperskillConnectorImpl : HyperskillConnector() {
  // Do not convert it into property with initializer
  // because [HYPERSKILL_URL] can be changed by user
  override val baseUrl: String get() = HYPERSKILL_URL
}
