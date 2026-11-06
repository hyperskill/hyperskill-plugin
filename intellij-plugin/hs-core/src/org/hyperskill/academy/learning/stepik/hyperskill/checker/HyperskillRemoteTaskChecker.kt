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
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.courseFormat.tasks.RemoteEduTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.UnsupportedTask
import org.hyperskill.academy.learning.map
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.hyperskill.api.HyperskillConnector
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
      is QuizTask -> HyperskillCheckConnector.checkQuizTask(project, task)
      is RemoteEduTask -> HyperskillCheckConnector.checkRemoteEduTask(project, task)
      is UnsupportedTask -> HyperskillCheckConnector.checkUnsupportedTask(task)
      else -> error("Can't check ${task.itemType} on ${EduNames.JBA}")
    }
  }

  override fun retry(task: Task): Result<Boolean, String> {
    HyperskillSettings.INSTANCE.account ?: return Err(EduCoreBundle.message("check.login.error", EduNames.JBA))
    if (task !is QuizTask) error("Can't retry ${task.itemType} on ${EduNames.JBA}")
    // A new attempt shuffles the answer options. The form is reloaded after the retry and takes the attempt from the server
    return HyperskillConnector.getInstance().postAttempt(task).map { attempt ->
      task.attempt = attempt
      task.reply = null
      true
    }
  }
}
