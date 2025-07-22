package org.hyperskill.academy.learning.stepik.hyperskill.checker

import org.hyperskill.academy.learning.EduActionTestCase
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.api.MockHyperskillConnector
import org.hyperskill.academy.learning.stepik.hyperskill.logInFakeHyperskillUser
import org.hyperskill.academy.learning.stepik.hyperskill.logOutFakeHyperskillUser

abstract class HyperskillActionTestBase : EduActionTestCase() {
  protected val mockConnector: MockHyperskillConnector
    get() = HyperskillConnector.getInstance() as MockHyperskillConnector

  override fun setUp() {
    super.setUp()
    logInFakeHyperskillUser()
  }

  override fun tearDown() {
    try {
      logOutFakeHyperskillUser()
    }
    catch (e: Throwable) {
      addSuppressedException(e)
    }
    finally {
      super.tearDown()
    }
  }
}
