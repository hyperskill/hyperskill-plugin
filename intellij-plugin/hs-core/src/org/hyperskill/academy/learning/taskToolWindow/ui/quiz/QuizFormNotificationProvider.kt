package org.hyperskill.academy.learning.taskToolWindow.ui.quiz

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotificationProvider
import org.hyperskill.academy.learning.JavaUILibrary
import org.hyperskill.academy.learning.courseFormat.tasks.QuizTask
import org.hyperskill.academy.learning.getContainingTask
import org.hyperskill.academy.learning.getTaskFile
import org.hyperskill.academy.learning.messages.EduCoreBundle
import org.hyperskill.academy.learning.stepik.hyperskill.quiz.QuizFormService
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowFactory
import org.hyperskill.academy.learning.taskToolWindow.ui.TaskToolWindowView
import org.hyperskill.academy.learning.taskToolWindow.ui.tab.TabType
import java.util.function.Function
import javax.swing.JComponent

/**
 * Points out the quiz form to a learner who looks for the place to answer in the editor, as in code steps.
 *
 * The files of a quiz step are only a sandbox, so the panel is shown in all of them, except the files the steps of some
 * types are answered with, see [QuizTask.ANSWER_FILES].
 */
class QuizFormNotificationProvider : EditorNotificationProvider, DumbAware {

  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!JavaUILibrary.isJCEF()) return null
    val task = file.getContainingTask(project) as? QuizTask ?: return null
    if (file.getTaskFile(project)?.name in QuizTask.ANSWER_FILES[task.blockName].orEmpty()) return null

    return Function { fileEditor ->
      if (fileEditor !is TextEditor) return@Function null
      EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Info).apply {
        text = EduCoreBundle.message("hyperskill.quiz.answer.in.form")
        createActionLabel(EduCoreBundle.message("hyperskill.quiz.go.to.form")) { showForm(project, task) }
      }
    }
  }

  private fun showForm(project: Project, task: QuizTask) {
    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow(TaskToolWindowFactory.STUDY_TOOL_WINDOW) ?: return
    TaskToolWindowView.getInstance(project).selectTab(TabType.DESCRIPTION_TAB)
    toolWindow.activate {
      QuizFormService.getInstance(project).form?.focus(task)
    }
  }
}
