package com.jetbrains.edu.learning.actions

import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.chains.SimpleDiffRequestChain
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.jetbrains.edu.learning.EduBrowser
import com.jetbrains.edu.learning.EduUtilsKt.isStudentProject
import com.jetbrains.edu.learning.actions.EduActionUtils.getCurrentTask
import com.jetbrains.edu.learning.courseFormat.ext.canShowSolution
import com.jetbrains.edu.learning.eduState
import com.jetbrains.edu.learning.stepik.hyperskill.HYPERSKILL_SOLUTIONS_ANCHOR
import com.jetbrains.edu.learning.stepik.hyperskill.hyperskillTaskLink
import org.jetbrains.annotations.NonNls

open class CompareWithAnswerAction : DumbAwareAction() {

  override fun actionPerformed(e: AnActionEvent) {
    val project = e.project ?: return
    val state = project.eduState ?: return

    val task = state.task

    val url = hyperskillTaskLink(task)
    EduBrowser.getInstance().browse("$url$HYPERSKILL_SOLUTIONS_ANCHOR")
  }

  protected open fun showSolution(project: Project, diffRequestChain: SimpleDiffRequestChain) {
    DiffManager.getInstance().showDiff(project, diffRequestChain, DiffDialogHints.FRAME)
  }

  override fun update(e: AnActionEvent) {
    val presentation = e.presentation
    presentation.isEnabledAndVisible = false
    val project = e.project ?: return
    if (!project.isStudentProject()) {
      return
    }
    val task = project.getCurrentTask() ?: return

    presentation.isEnabledAndVisible = task.canShowSolution()
  }

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.EDT

  companion object {
    @NonNls
    const val ACTION_ID = "Educational.CompareWithAnswer"
  }
}