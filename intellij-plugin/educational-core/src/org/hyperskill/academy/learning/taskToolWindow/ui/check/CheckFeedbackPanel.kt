package org.hyperskill.academy.learning.taskToolWindow.ui.check

import com.intellij.ui.components.JBLabel
import com.intellij.util.Alarm
import com.intellij.util.text.DateFormatUtil
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import org.hyperskill.academy.EducationalCoreIcons.CheckPanel.ResultCorrect
import org.hyperskill.academy.EducationalCoreIcons.CheckPanel.ResultIncorrect
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.CheckStatus
import org.hyperskill.academy.learning.courseFormat.tasks.IdeTask
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.courseFormat.tasks.TheoryTask
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.taskToolWindow.ui.check.CheckMessagePanel.Companion.FOCUS_BORDER_WIDTH
import org.hyperskill.academy.learning.ui.EduColors
import java.awt.BorderLayout
import java.util.*
import java.util.concurrent.TimeUnit
import javax.swing.JPanel

class CheckFeedbackPanel(task: Task, checkResult: CheckResult, alarm: Alarm) : JPanel(BorderLayout()) {
  init {
    if (checkResult.status != CheckStatus.Unchecked) {
      add(ResultLabel(task, checkResult), BorderLayout.WEST)
    }
    val checkTime = task.feedback?.time
    if (checkTime != null) {
      add(TimeLabel(checkTime, alarm), BorderLayout.CENTER)
    }
  }

  override fun isVisible(): Boolean = componentCount > 0

  private class ResultLabel(task: Task, checkResult: CheckResult) : JBLabel() {
    init {
      val status = checkResult.status

      iconTextGap = JBUI.scale(4)
      icon = when (status) {
        CheckStatus.Failed -> ResultIncorrect
        CheckStatus.Solved -> ResultCorrect
        else -> null
      }
      foreground = when (status) {
        CheckStatus.Failed -> EduColors.wrongLabelForeground
        CheckStatus.Solved -> EduColors.correctLabelForeground
        else -> foreground
      }

      text = when (status) {
        CheckStatus.Failed -> EduCoreBundle.message("check.incorrect")
        CheckStatus.Solved -> when (task) {
          is IdeTask, is TheoryTask -> EduCoreBundle.message("check.done")
          else -> EduCoreBundle.message("check.correct")
        }

        else -> ""
      }
      border = JBUI.Borders.empty(16, FOCUS_BORDER_WIDTH, 0, 16 - FOCUS_BORDER_WIDTH)
    }
  }

  private class TimeLabel(private val time: Date, private val alarm: Alarm) : JBLabel() {
    init {
      border = JBUI.Borders.empty(16, FOCUS_BORDER_WIDTH, 0, 0)
      foreground = UIUtil.getLabelDisabledForeground()

      val timeUpdater = object : Runnable {
        override fun run() {
          text = DateFormatUtil.formatPrettyDateTime(time)
          alarm.addRequest(this, TimeUnit.MINUTES.toMillis(1))
        }
      }
      timeUpdater.run()
    }
  }
}