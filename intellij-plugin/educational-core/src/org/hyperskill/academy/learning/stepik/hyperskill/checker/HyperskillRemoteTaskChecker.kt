package org.hyperskill.academy.learning.stepik.hyperskill.checker

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import org.hyperskill.academy.learning.EduNames
import org.hyperskill.academy.learning.Err
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.checker.remote.RemoteTaskChecker
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.hyperskill.HyperskillCourse
import org.hyperskill.academy.learning.courseFormat.tasks.CodeTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.UnsupportedTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.hyperskill.settings.HyperskillSettings

class HyperskillRemoteTaskChecker : RemoteTaskChecker {
  override fun canCheck(project: Project, task: Task): Boolean {
    return task.course is HyperskillCourse && HyperskillCheckConnector.isRemotelyChecked(task)
  }

  override fun check(project: Project, task: Task, indicator: ProgressIndicator): CheckResult {
    HyperskillSettings.INSTANCE.account ?: return CheckResult(
      CheckStatus.Unchecked,
      EduCoreBundle.message("check.login.error", EduNames.JBA)
    )
    return when (task) {
      is CodeTask -> HyperskillCheckConnector.checkCodeTask(project, task)
      is RemoteEduTask -> HyperskillCheckConnector.checkRemoteEduTask(project, task)
      is UnsupportedTask -> HyperskillCheckConnector.checkUnsupportedTask(task)
      else -> error("Can't check ${task.itemType} on ${EduNames.JBA}")
    }
  }

  override fun retry(task: Task): Result<Boolean, String> {
    HyperskillSettings.INSTANCE.account ?: Err(EduCoreBundle.message("check.login.error", EduNames.JBA))
    error("Can't retry ${task.itemType} on ${EduNames.JBA}")
  }
}
