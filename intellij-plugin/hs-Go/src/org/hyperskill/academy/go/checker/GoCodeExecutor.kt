package org.hyperskill.academy.go.checker

import com.goide.psi.GoFile
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import org.hyperskill.academy.go.checker.GoEduTaskChecker.Companion.GO_RUN_WITH_PTY
import org.hyperskill.academy.learning.Result
import org.hyperskill.academy.learning.checker.DefaultCodeExecutor
import org.hyperskill.academy.learning.courseFormat.CheckResult
import org.hyperskill.academy.learning.courseFormat.ext.getDocument
import org.hyperskill.academy.learning.courseFormat.tasks.Task
import org.hyperskill.academy.learning.toPsiFile
import org.hyperskill.academy.learning.withRegistryKeyOff

class GoCodeExecutor : DefaultCodeExecutor() {
  override fun createRunConfiguration(project: Project, task: Task): RunnerAndConfigurationSettings? {
    val psiFile = getMainFile(project, task) ?: return null
    return ConfigurationContext(psiFile).configuration
  }

  override fun execute(project: Project, task: Task, indicator: ProgressIndicator, input: String?): Result<String, CheckResult> {
    return withRegistryKeyOff(GO_RUN_WITH_PTY) { super.execute(project, task, indicator, input) }
  }

  private fun getMainFile(project: Project, task: Task): PsiFile? {
    for ((_, file) in task.taskFiles) {
      val psiFile = file.getDocument(project)?.toPsiFile(project) ?: continue
      if (psiFile is GoFile && psiFile.hasMainFunction()) {
        return psiFile
      }
    }
    return null
  }
}