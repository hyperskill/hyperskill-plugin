package org.hyperskill.academy.learning.stepik.hyperskill.checker

import org.hyperskill.academy.learning.actions.CheckAction
import org.hyperskill.academy.learning.checker.CheckActionListener
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.navigation.NavigationUtils
import org.hyperskill.academy.learning.testAction
import org.hyperskill.academy.learning.ui.getUICheckLabel

abstract class HyperskillCheckActionTestBase : HyperskillActionTestBase() {

  override fun setUp() {
    super.setUp()
    CheckActionListener.registerListener(testRootDisposable)
  }

  protected fun checkCheckAction(task: Task, expectedStatus: CheckStatus, expectedMessage: String? = null) {
    when (expectedStatus) {
      CheckStatus.Unchecked -> CheckActionListener.shouldSkip()
      CheckStatus.Solved -> CheckActionListener.reset()
      CheckStatus.Failed -> CheckActionListener.shouldFail()
    }
    if (expectedMessage != null) {
      CheckActionListener.expectedMessage { expectedMessage }
    }

    NavigationUtils.navigateToTask(project, task)
    testAction(CheckAction(task.getUICheckLabel()))
  }
}
