package org.hyperskill.academy.scala.sbt.checker

import com.intellij.execution.testframework.sm.runner.SMTestProxy
import org.hyperskill.academy.learning.checker.tests.SMTestResultCollector
import org.hyperskill.academy.learning.xmlEscaped

class ScalaTestResultCollector : SMTestResultCollector() {
  override fun getErrorMessage(node: SMTestProxy): String = super.getErrorMessage(node).xmlEscaped
}
