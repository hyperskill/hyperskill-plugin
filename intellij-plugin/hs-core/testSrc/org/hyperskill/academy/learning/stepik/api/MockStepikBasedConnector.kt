package org.hyperskill.academy.learning.stepik.api

import com.intellij.openapi.Disposable
import org.hyperskill.academy.learning.ResponseHandler

interface MockStepikBasedConnector {
  fun withResponseHandler(disposable: Disposable, handler: ResponseHandler): MockStepikBasedConnector
}