package org.hyperskill.academy.python.slow.checker

import org.hyperskill.academy.learning.checker.CheckersTestBase
import org.hyperskill.academy.learning.checker.EduCheckerFixture
import org.hyperskill.academy.python.learning.newproject.PyProjectSettings

// This test runs only when PYTHON_SDK environment variable is defined and points to the valid python interpreter.
abstract class PyCheckersTestBase : CheckersTestBase<PyProjectSettings>() {
  override fun createCheckerFixture(): EduCheckerFixture<PyProjectSettings> = PyCheckerFixture()
}
