package org.hyperskill.academy.learning.stepik.hyperskill

import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector

/**
 * Alternative REST service that handles requests on `/api/hyperskill` path
 * (without the `/edu/` prefix).
 *
 * This is used for gradual migration from `/api/edu/hyperskill` to `/api/hyperskill`.
 * Inherits all behavior from [HyperskillRestService], only overrides the service name.
 */
class HyperskillRestServiceAlternative : HyperskillRestService() {

  override fun getServiceName(): String = HyperskillConnector.getInstance().alternativeServiceName
}
