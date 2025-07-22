package org.hyperskill.academy.coursecreator.validation

import com.intellij.execution.testframework.sm.ServiceMessageBuilder

interface ServiceMessageConsumer {
  fun consume(message: ServiceMessageBuilder)
}
